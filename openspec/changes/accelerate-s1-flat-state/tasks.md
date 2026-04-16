Implementation order matches the checkpoints in `design.md`. Each section ends with a benchmark/acceptance gate — don't advance until the gate passes.

## 0. Prerequisite audits

**Chosen S1 replays (for every S1-touching bench/test/gate in this change):**
- **Dota (S1)**: `replays/dota/s1/normal/271145478.dem`
- **CSGO (S1)**: `replays/csgo/s1/luminosity-vs-azio-cache.dem`

Use both wherever a single-replay line is mentioned; S1 code serves both engines (pre-Source-2 Dota and CS:GO 1), so parity/perf must be proven on both.

- [x] 0.1 **Smoke-parse both S1 replays on the pre-change `next` tip.** Before any code changes: run `:repro:issue289Run` or a minimal `SimpleRunner` + `@UsesEntities` empty processor against each replay path above on the current `next` HEAD. Both SHALL complete without exceptions. This establishes that the actual baseline this change starts from handles both replays cleanly and that any later failure is attributable to this change's code.

- [x] 0.2 **S1 parse-bench baseline harness** — today's `EntityStateParseBench` is S2-only (`@Param({"NESTED_ARRAY", "TREE_MAP", "FLAT"})` + `withS2EntityState`). Either extend it with an S1 axis or add sibling `S1EntityStateParseBench` parametrized `@Param({"OBJECT_ARRAY", "FLAT"})` × `@Param` of {dota, csgo} replay paths. Runs `SimpleRunner` against an empty `@UsesEntities` processor. Capture CP-0 wall-clock + `-prof gc` alloc on the pre-change `next` tip before the port starts (OBJECT_ARRAY only at this point — FLAT lands in CP-1).

- [x] 0.3 **Baseline event-stream capture** — run a simple harness that dumps `@OnEntityUpdated` entity-index + fieldpath idx + reflected property value (via `entity.getProperty`) + `@OnEntityCreated`/`@OnEntityDeleted` order to deterministic text files for each chosen S1 replay against the pre-change `next` tip. These are the parity targets for CP-2's new path. Reuse or adapt the `SmokeTraceMain` shape from `src/jmh/java/skadistats/clarity/bench/`. Commit the captures to an ignored scratch location (e.g. `/tmp/`) or record the file hashes in this task for reproducible verification.

- [x] 0.4 **Inline-string budget sanity check** — run `:dev:s1sendtables` against the Dota replay (already produces `s1sendprops_-1.txt`); compute per-DTClass worst-case `Σ (1 + 2 + maxLength)` over STRING props (uniform `maxLength = 512`). Log DTClasses above 64 KB so CP-1's construction path can warn on them (no runtime action needed — informational only).

## 1. S1FlatLayout + S1FlatEntityState + tests (CP-1)

- [x] 1.1 Create `skadistats.clarity.model.state.S1FlatLayout` per `design.md` D1. Fields: `LeafKind[] kinds`, `int[] offsets`, `PrimitiveType[] primTypes`, `int[] maxLengths`, `int dataBytes`, `int refSlots`.

- [x] 1.2 Create `S1FlatLayout.Builder` (or a static `build(ReceiveProp[])`) that iterates props, classifies each via `decoder.getPrimitiveType()` / `instanceof StringLenDecoder`, assigns offsets, and produces an immutable `S1FlatLayout`. Routing from D1:
  - `getPrimitiveType() != null` → `PRIMITIVE(primType)`, slot size = `primType.size()`, offset advances by `1 + primType.size()`.
  - `instanceof StringLenDecoder` → `INLINE_STRING`, slot size = `2 + 512` (maxLength uniform), offset advances by `1 + 2 + 512`.
  - Otherwise (`ArrayDecoder`, `PointerDecoder`, anything else that slips through) → `REF`, slot size = 4 (int slot index), offset advances by `1 + 4`. Increment `refSlots`.

- [x] 1.3 Wire `S1DTClass.getFlatLayout()` — build + cache on first access (or eagerly from the `S1DTClass` construction path if reachable without circularity; see design Open Questions).

- [x] 1.4 Create `skadistats.clarity.model.state.S1FlatEntityState` per D2:
  - Fields: `S1FlatLayout layout`, `byte[] data`, `Object[] refs` (nullable), `int[] freeSlots` (nullable), `int freeSlotsTop`.
  - Constructor `(S1DTClass dtClass)` — allocate `new byte[layout.dataBytes()]`; if `layout.refSlots() > 0`, allocate `new Object[Math.max(4, layout.refSlots())]` + `new int[refs.length]`.
  - Copy constructor — eager deep copy per D10.
  - `copy()` delegates.

- [x] 1.5 Implement `S1FlatEntityState.decodeInto(fp, decoder, bs)` per D3. Single switch on `layout.kinds()[idx]`:
  - `PRIMITIVE` → `data[offset] = 1; DecoderDispatch.decodeInto(bs, decoder, data, offset + 1);`
  - `INLINE_STRING` → `data[offset] = 1; StringLenDecoder.decodeIntoInline(bs, data, offset + 1, layout.maxLengths()[idx]);`
  - `REF` → `throw new IllegalStateException("decodeInto called on REF leaf, idx=" + idx);`
  - Return `false` (S1 never reports `capacityChanged`).

- [x] 1.6 Implement `S1FlatEntityState.write(fp, decoded)` per D4. Switch on `layout.kinds()[idx]`:
  - `PRIMITIVE` → `data[offset] = 1; layout.primTypes()[idx].write(data, offset + 1, decoded);`
  - `INLINE_STRING` → `data[offset] = 1; writeInlineString(offset + 1, (String) decoded, layout.maxLengths()[idx]);` (UTF-8 encode, write 2-byte LE length prefix + bytes, truncate at `maxLength`).
  - `REF` — flag-byte-gated slot allocation; on flag=0 allocate via `allocateRefSlot()`, set flag, write slot-idx via `INT_VH.set`, store in `refs`. On flag=1 read existing slot, overwrite `refs[slot]`.
  - Return `false`.
  - Helpers: `allocateRefSlot()` (pop `freeSlots` or append + grow `refs` via `Arrays.copyOf`), `freeRefSlot(int)` (push onto `freeSlots`).

- [x] 1.7 Implement `S1FlatEntityState.applyMutation(fp, mutation)` per D5 — cast to `StateMutation.WriteValue`, delegate to `write(fp, wv.value())`. Non-`WriteValue` paths throw via the cast (matches current S1 behaviour — S1 has no `ResizeVector` / `SwitchPointer`).

- [x] 1.8 Implement `S1FlatEntityState.getValueForFieldPath(fp)` per D6. Return `null` when flag byte is 0. Otherwise dispatch on leaf kind; for `INLINE_STRING`, read 2-byte LE length prefix and allocate `new String(data, offset + 3, len, StandardCharsets.UTF_8)`.

- [x] 1.9 Implement `S1FlatEntityState.fieldPathIterator` per D7 — iterate `0..layout.kinds.length - 1` unconditionally, yielding `new S1FieldPath(i)`. Matches `ObjectArrayEntityState` semantic.

- [x] 1.10 Add `S1EntityStateType.FLAT` enum variant alongside existing `OBJECT_ARRAY` (don't flip the default yet — CP-1 is additive). `FLAT.createState(...)` returns `new S1FlatEntityState(dtClass)`. If the signature needs updating to pass `S1DTClass` rather than `ReceiveProp[]`, update `EntityStateFactory` and all call sites. `OBJECT_ARRAY.createState` remains on the receive-props path (behaviour unchanged).

- [x] 1.11a Add `ObjectArrayEntityState.decodeInto(fp, decoder, bs)` — one-liner: `state[fp.s1().idx()] = DecoderDispatch.decode(bs, decoder); return false;`. Keeps the reader's call shape uniform across both variants. `OBJECT_ARRAY` still pays the full boxing cost; this is wiring, not an optimization.

- [x] 1.11 Unit tests in `src/test/java/skadistats/clarity/model/state/S1FlatEntityStateTest.java`:
  - `copyIsIndependent` — write to copy, original unchanged; write to original, copy unchanged. Cover PRIMITIVE, INLINE_STRING, REF.
  - `primitiveRoundTrip` — for each `PrimitiveType.Scalar` + a representative `VectorType`, write via `write`, read via `getValueForFieldPath`, assert equal.
  - `inlineStringRoundTrip` — seed a synthetic `BitStream` with a known string payload in `StringLenDecoder` wire format; `decodeInto` into state; `getValueForFieldPath` returns the same `String`. Bonus: parity against `StringLenDecoder.decode` on a parallel stream.
  - `refSlotLifecycle` — `write` to a REF slot allocates (flag 0→1), subsequent `write` overwrites without re-allocating. Iterate a few slots to cover freelist reuse.
  - `fieldPathIteratorMatchesObjectArrayEntityState` — build parallel states, assert iterators yield the same sequence of field paths.
  - `applyMutationDelegatesToWrite` — `applyMutation(fp, new WriteValue(v))` behaves identically to `write(fp, v)`.

- [x] 1.12 **Gate: unit tests green.** `./gradlew :clarity:test` passes. `:repro:issue289Run` + `:repro:issue350Run` still green against default `OBJECT_ARRAY`.

## 2. Port `S1FieldReader` + flip default to FLAT (CP-2)

- [x] 2.1 Extract the current `S1FieldReader.readFields` body into a `readFieldsDebug(bs, dtClass, state)` helper (unchanged logic) and a `readFieldsMaterialize(bs, dtClass, state)` helper (produces `FieldChanges.mutations[]` via boxed `DecoderDispatch.decode` + `new WriteValue(...)`, no `TextTable`). Both return `FieldChanges` populated with the legacy staging shape.

- [x] 2.2 Rewrite the fast-path in `S1FieldReader.readFields` per D8:
  ```java
  if (debug)        return readFieldsDebug(bs, dtClass, state);
  if (materialize)  return readFieldsMaterialize(bs, dtClass, state);
  var state = (S1FlatEntityState) stateGeneric;
  var layout = dtClass.getFlatLayout();
  var receiveProps = dtClass.getReceiveProps();
  var n = readIndices(bs, dtClass);
  for (var ci = 0; ci < n; ci++) {
      var o = fieldPaths[ci].s1().idx();
      var decoder = receiveProps[o].getSendProp().getDecoder();
      if (layout.kinds()[o] == LeafKind.REF) {
          state.write(fieldPaths[ci], DecoderDispatch.decode(bs, decoder));
      } else {
          state.decodeInto(fieldPaths[ci], decoder, bs);
      }
  }
  return new FieldChanges(fieldPaths, n, /* capacityChanged */ false);
  ```

- [x] 2.3 Run the CP-0 event-stream capture against an explicit `withS1EntityState(FLAT)` run; diff against the pre-change baseline from 0.3 (captured on `OBJECT_ARRAY`). Fail the gate on any byte-level difference. Fix parity bugs in `S1FlatEntityState.getValueForFieldPath` / `S1FlatLayout` build order / etc. until diff is empty.

- [x] 2.4 Run `:repro:issue289Run`, `:repro:issue350Run`, and every `:examples:*Run` that touches an S1 replay (matchend, allchat against a pre-Source-2 replay if coverage exists) — once on default, once with `.withS1EntityState(OBJECT_ARRAY)`. Both paths green with no exceptions.

- [x] 2.5 Flip `AbstractFileRunner.s1EntityStateType` default from `OBJECT_ARRAY` to `FLAT`. Confirm no other default-picking path exists (grep for `OBJECT_ARRAY` across main sources + tests; residual usages are fine as long as they are explicit opt-in).

- [x] 2.6 **Gate: `S1EntityStateParseBench` on FLAT vs OBJECT_ARRAY.** `@Param({"OBJECT_ARRAY", "FLAT"})`. Wall-clock: FLAT `≥ -20%` vs OBJECT_ARRAY baseline. `-prof gc` alloc: FLAT `≥ -50%` vs OBJECT_ARRAY. Document any deviation below the threshold; investigate before advancing. OBJECT_ARRAY numbers are retained as the baseline side of the bench long-term.

- [x] 2.7 **Gate: S2 benches unchanged.** `EntityStateParseBench` on CS2/Deadlock/Dota 2 shows no regression vs pre-change — confirms the port is isolated.

## 3. Downstream compatibility (CP-3)

- [x] 3.1 `clarity-analyzer` (at `/home/spheenik/projects/clarity/clarity-analyzer`, `next` branch, composite build `includeBuild("../clarity")`): run `./gradlew compileJava` / equivalent. Must compile against modified clarity sources. Interactive run gate is user-verified on request.

- [x] 3.2 `MutationTraceBench` (in `src/jmh/java/skadistats/clarity/bench/`): capture a trace on each chosen S1 replay via `MutationRecorder`; materialize + replay via `BirthMaterializer`. Assert no exceptions and byte-for-byte parity on the replayed event stream. Adapt `SmokeTraceMain` for S1 if it's S2-only today. Run for both Dota (`271145478.dem`) and CSGO (`luminosity-vs-azio-cache.dem`).

- [x] 3.3 `./gradlew :clarity:build` — full clarity + test suite green on HEAD. No regressions vs CP-2.

- [x] 3.4 **Gate: both clarity-analyzer + MutationTraceBench round-trip green.**

## 4. Final validation + results

- [x] 4.1 Consolidate numbers into `RESULTS.md` alongside `proposal.md`:
  - Headline: S1 `EntityStateParseBench` wall-clock + alloc delta (FLAT vs OBJECT_ARRAY) on both Dota and CSGO replays.
  - Inline-string memory footprint histogram from audit 0.4.
  - Per-CP gate results.
  - Acceptance checklist (all gates PASS).
  - Follow-up tracking (S2 class `S2*` prefix rename, inline ARRAY props, cached-String companion).

- [x] 4.2 `openspec validate accelerate-s1-flat-state --strict` — passes.

- [x] 4.3 `./gradlew build` — full suite green on HEAD.

- [x] 4.4 Update `proposal.md` "Performance expectations" section with actual measured numbers.

- [x] 4.5 **Gate: ready to archive.** Invoke `/opsx:archive` when all above pass.
