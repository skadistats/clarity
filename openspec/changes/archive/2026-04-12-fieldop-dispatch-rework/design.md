## Context

The S2 entity decode hot path processes field operations via `S2FieldReader.readFields()`. Each iteration of the inner loop calls `bs.readFieldOp()` (returns a `FieldOpType` enum constant) followed by `op.execute(mfp, bs)` (virtual dispatch to one of 40 anonymous enum subclasses).

JIT PrintInlining confirms:
- `FieldOpType::execute` at bytecode offset 63: **"virtual call"**, **"no static binding"** — zero inlining
- `BitStream64::readFieldOp` (228 bytes): **"callee is too large"** — not inlined into readFields
- `S2FieldReader::readFields` (621 bytes): **"hot method too big"** — not inlined into callers

async-profiler CPU profile shows `itable stub` (2.91%) + `vtable stub` (2.50%) + individual `FieldOpType$N.execute` methods (~3%) = ~8-10% of mutator-thread CPU on virtual dispatch overhead.

The Huffman frequency distribution is heavily skewed: PlusOne (36271), FieldPathEncodeFinish (25474), PushOneLeftDeltaNRightNonZeroPack6Bits (10530), PlusTwo (10334) account for ~80% of all ops. Branch prediction for a switch jump table will exploit this skew effectively.

## Goals / Non-Goals

**Goals:**
- Eliminate virtual dispatch on the `execute()` call site in the field-op loop
- Allow HotSpot to inline field-op bodies directly into the compiled loop
- Reduce `readFields` bytecode size on the fast path to improve JIT optimization budget
- Maintain identical parse behavior — bit-exact same output for all replays

**Non-Goals:**
- Changing the Huffman decode itself (`readFieldOp` / lookup table) — already well-optimized
- Removing `FieldOpType` enum — it remains as the canonical source for weights, ordinal constants, and debug display
- Optimizing `S2ModifiableFieldPath` operations (inc, down, up) — separate concern
- Reducing allocation pressure (FieldChanges, S2LongFieldPath, etc.) — separate concern
- Changing any public API surface

## Decisions

### 1. Switch on int ordinal, not on enum constant

**Choice:** `readFieldOpId()` returns `int` (the Huffman-resolved ordinal); the switch dispatches on that int.

**Alternative considered:** `switch(op) { case PlusOne: ... }` using the enum directly. This compiles to an internal `$SwitchMap` synthetic array + `ordinal()` call, adding an extra indirection. Returning the raw int from the Huffman decoder is one fewer array load per iteration and maps directly to `tableswitch` bytecode.

**How:** `FieldOpHuffmanTree` already stores ordinals internally. `readFieldOp()` currently does `return FieldOpHuffmanTree.ops[(entry >>> 8) & 0xFF]` — the new `readFieldOpId()` simply returns `(entry >>> 8) & 0xFF` directly. Both fast path and slow path in `BitStream64.readFieldOp()` are adapted similarly.

### 2. Static final int constants for op ordinals

**Choice:** Add `public static final int` constants to `FieldOpType` (e.g., `FieldOpType.OP_PLUS_ONE = 0`) derived from `ordinal()` in a static initializer.

**Alternative considered:** Use raw literal ints in the switch. Rejected because it's unmaintainable — if enum order ever changes, the switch silently breaks.

**Alternative considered:** Use `FieldOpType.PlusOne.ordinal()` directly in case labels. Not possible — Java requires constant expressions in case labels, and `ordinal()` is not a compile-time constant.

**How:** A static block in `FieldOpType` populates `public static final int OP_PLUS_ONE = PlusOne.ordinal()` etc. These are compile-time-foldable by the JIT even though they're runtime-initialized.

### 3. Split readFields into fast and debug variants

**Choice:** `readFields(bs, dtClass, debug)` dispatches to `readFieldsFast(bs, dtClass)` or `readFieldsDebug(bs, dtClass)` based on the debug flag.

**Alternative considered:** Keep a single method with `if (debug)` checks. Rejected because the debug code (TextTable setup, string formatting per-op) bloats the bytecodes seen by HotSpot. The fast variant without any debug code will be significantly smaller and more amenable to optimization.

**How:** The fast variant contains only the switch loop + decoder loop. The debug variant is a copy with added instrumentation. Code duplication is acceptable here because the methods are structurally coupled and rarely change.

### 4. FieldPathEncodeFinish handling inside the switch

**Choice:** `FieldPathEncodeFinish` is a regular case in the switch that triggers loop exit (via labeled break or direct return).

**Alternative considered:** Check for finish _after_ the switch via `if (opId == FieldOpType.OP_FIELD_PATH_ENCODE_FINISH)`. Rejected — adds a branch per iteration that the switch already handles.

## Risks / Trade-offs

**[Risk] Switch body makes readFieldsFast large** — 40 cases × ~2-5 lines each = ~200 lines of switch body. Combined with the decoder loop, the method may still exceed FreqInlineSize (325 bytes). → **Mitigation:** This only affects whether readFieldsFast gets inlined into _its caller_ (Entities.processPacketEntities), which is a minor win. The primary win — eliminating virtual dispatch _within_ the loop — is independent of whether the method itself is inlined by callers.

**[Risk] Indirect branch misprediction on the switch** — A `tableswitch` compiles to an indirect branch, which has prediction cost when the target varies. → **Mitigation:** The Huffman frequency distribution is heavily skewed (~80% concentrated in 4 ops). Modern branch predictors handle this well. Empirically measurable via benchmark.

**[Risk] Code duplication between fast and debug paths** — Two copies of the core loop logic. → **Mitigation:** The debug path is rarely executed (only when logback level is set). The fast path is the one that matters for correctness and perf. Both paths must produce identical parse results — verifiable by running the entitybaseline benchmark against both paths.

**[Risk] Enum ordinal stability** — If someone reorders `FieldOpType` enum constants, the static `OP_*` ints shift. → **Mitigation:** The ints are derived from `ordinal()` at static-init time, so they always match. The Huffman tree also uses `ordinal()`. No manual sync needed.
