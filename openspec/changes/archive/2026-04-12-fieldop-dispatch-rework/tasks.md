## 1. Add int-based field op ID infrastructure

- [x] 1.1 Add `public static final int OP_*` ordinal constants to `FieldOpType` for all 40 ops
- [x] 1.2 Add `abstract int readFieldOpId()` to `BitStream`
- [x] 1.3 Implement `readFieldOpId()` in `BitStream64` — same logic as `readFieldOp()` but return `(entry >>> 8) & 0xFF` / `(-i - 1)` directly instead of array lookup
- [x] 1.4 Implement `readFieldOpId()` in `BitStream32` (if it exists and has a readFieldOp override)

## 2. Refactor S2FieldReader into fast and debug paths

- [x] 2.1 Extract `readFieldsFast(BitStream, S2DTClass)` — contains the field-op switch loop + decoder loop, no debug code
- [x] 2.2 Extract `readFieldsDebug(BitStream, S2DTClass)` — copy of fast path with TextTable instrumentation retained
- [x] 2.3 Make `readFields(BitStream, DTClass, boolean)` delegate to fast or debug variant based on the debug flag

## 3. Replace virtual dispatch with switch in the field-op loop

- [x] 3.1 In `readFieldsFast`: replace `op.execute(mfp, bs)` with a `switch(bs.readFieldOpId())` containing all 40 case bodies
- [x] 3.2 Handle `FieldPathEncodeFinish` as a case that breaks out of the loop directly
- [x] 3.3 Port the same switch structure into `readFieldsDebug`, preserving TextTable recording per op

## 4. Verify correctness and measure

- [x] 4.1 Run `entitybaselineRun` against `replays/dota/s2/349/8607273484_1672844511.dem` — must complete without errors
- [x] 4.2 Run the full examples test suite (`./gradlew build` in clarity-examples) — must compile and pass
- [x] 4.3 Run async-profiler CPU benchmark (same setup as explore session) — compare wall-clock avg and itable/vtable stub percentages before vs after
- [x] 4.4 Run JIT PrintInlining on the benchmark — confirm `readFieldOpId` and switch bodies are inlined, no more "virtual call" at offset 63
