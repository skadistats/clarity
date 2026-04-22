## 1. Rename existing classes

- [x] 1.1 Rename `PointerField` → `PolymorphicPointerField` (class, file, package references)
- [x] 1.2 Rename `PointerDecoder` → `PolymorphicPointerDecoder` (class, file, `@RegisterDecoder` annotation, `DecoderDispatch` generated dispatch)
- [x] 1.3 Update `S2FieldReader`: `instanceof PointerField` → `instanceof PolymorphicPointerField`, cast on line 149 likewise
- [x] 1.4 Update `FieldGenerator` import and construction site for the polymorphic case
- [x] 1.5 Verify `clarity-analyzer` compiles after renames

## 2. Simplify PolymorphicPointerDecoder

- [x] 2.1 Remove the `types.length > 1` guard — always read the `ubitvar` after a `true` presence bit
- [x] 2.2 Confirm `PolymorphicPointerDecoder` is only instantiated from `FieldGenerator` when `typeSerializers.length > 1`

## 3. Implement FixedPointerDecoder

- [x] 3.1 Create `FixedPointerDecoder` with `@RegisterDecoder`; `decode()` reads one bit and returns `Boolean`
- [x] 3.2 Regenerate (or manually update) `DecoderDispatch` to include the new decoder

## 4. Implement FixedPointerField

- [x] 4.1 Create `FixedPointerField` as a `permits` entry in the sealed `Field` hierarchy
- [x] 4.2 Store the single `Serializer` as a final field; `resolveSerializer()` returns it directly
- [x] 4.3 `getChild(S2EntityState, int)` delegates to the fixed serializer without touching state
- [x] 4.4 `getChildIndex` and `getChildNameSegment` likewise use the fixed serializer directly
- [x] 4.5 `createMutation()` returns `SwitchFixedPointer(boolean)` based on presence bit
- [x] 4.6 `prepareForWrite()` returns serializer when present, null when absent
- [x] 4.7 `isHiddenFieldPath()` returns `true` (same as `PolymorphicPointerField`)
- [x] 4.8 `getDecoder()` returns the `FixedPointerDecoder` instance

## 5. Wire up FieldGenerator

- [x] 5.1 In `createField()`, when `typeSerializers.length == 1` construct `FixedPointerField` and skip `pointerCount++`
- [x] 5.2 Confirm `pointerCount` reflects only truly polymorphic fields after the change

## 6. Verify

- [x] 6.1 Run `pointerstats` dev tool against all three S2 benchmark replays; confirm counts unchanged
- [x] 6.2 Run `./gradlew build` across clarity, clarity-examples, and clarity-analyzer
- [x] 6.3 Parse the CS2 benchmark replay end-to-end and confirm `m_pGameModeRules` still resolves correctly
