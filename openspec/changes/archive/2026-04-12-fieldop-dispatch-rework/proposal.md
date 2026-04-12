## Why

The `FieldOpType` enum uses 40 anonymous subclasses with overridden `execute()` methods. JIT PrintInlining confirms the call site in `S2FieldReader.readFields` at bytecode offset 63 is a **virtual call** with **no static binding** — HotSpot cannot inline any of the 40 implementations. This causes `itable stub` (2.91%) and `vtable stub` (2.50%) overhead in CPU profiles, totaling ~8-10% of mutator-thread wall-clock time. Replacing the polymorphic dispatch with a `switch` on the op ordinal eliminates virtual calls entirely and lets HotSpot compile the hot path as a single inlined method body with a jump table.

## What Changes

- Replace `FieldOpType.execute()` virtual dispatch with a `switch(opId)` in the field-op processing loop
- Add `readFieldOpId()` to BitStream that returns the raw int ordinal instead of the enum constant, avoiding the enum array lookup on the hot path
- Split `S2FieldReader.readFields` into a fast path (no debug) and a debug path to reduce bytecode size below HotSpot's `FreqInlineSize` threshold (currently 621 bytes, threshold is 325)
- Keep `FieldOpType` enum intact for debug output, symbolic constants, and Huffman tree weight definitions — no external API change
- **BREAKING**: None. Internal refactor only. The `@OnMessage` event API and all public processor interfaces remain unchanged.

## Capabilities

### New Capabilities
- `fieldop-switch-dispatch`: Switch-based field op execution replacing virtual dispatch in the S2 entity decode hot path

### Modified Capabilities

## Impact

- `S2FieldReader.java` — major rewrite of `readFields` method, split into fast/debug variants
- `BitStream.java` / `BitStream64.java` / `BitStream32.java` — new `readFieldOpId()` method returning int
- `FieldOpType.java` — `execute()` method bodies migrated to switch cases; enum constants kept for weights and debug
- `FieldOpHuffmanTree.java` — may need adjustment if ops array access changes
- No dependency changes. No public API changes. No wire format changes.
