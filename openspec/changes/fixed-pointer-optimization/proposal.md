## Why

Every pointer field with a single possible serializer allocates a `pointerSerializers` slot in every entity state instance and performs a redundant array lookup on the hot path, even though the serializer can never change. Separating fixed from polymorphic pointer fields eliminates this overhead and simplifies the field navigation path.

## What Changes

- New `FixedPointerField` — stores the serializer directly, no `pointerId` assigned, `resolveSerializer()` is a direct field read, `createMutation()` returns null, `prepareForWrite()` returns null
- New `FixedPointerDecoder` — reads one bit (presence boolean only), returns `Boolean`; no `SerializerId[]` types array needed
- Rename `PointerField` → `PolymorphicPointerField`
- Rename `PointerDecoder` → `PolymorphicPointerDecoder`; remove the `types.length > 1` branch — it always reads the ubitvar since it is only instantiated for multi-type fields
- `FieldGenerator` routes single-serializer pointer fields to `FixedPointerField`, multi-serializer to `PolymorphicPointerField`
- `pointerSerializers[]` in `S2EntityState` shrinks to only truly polymorphic pointer fields (0 slots for Dota/Deadlock, 1 for CS2 today)
- `S2FieldReader.pointerOverrides[]` shrinks by the same count

## Capabilities

### New Capabilities

- `fixed-pointer-field`: Behavior and contract of `FixedPointerField` and `FixedPointerDecoder` — wire format consumed, decoded value, mutation contract, and field navigation semantics

### Modified Capabilities

(none — `PolymorphicPointerField` behavior is identical to the current `PointerField`; only naming and instantiation condition change)

## Impact

- `skadistats.clarity.model.s2.field.PointerField` — renamed to `PolymorphicPointerField` (**BREAKING** for any downstream code referencing the class by name)
- `skadistats.clarity.io.decoder.PointerDecoder` — renamed to `PolymorphicPointerDecoder` (**BREAKING** for downstream)
- `skadistats.clarity.io.s2.S2FieldReader` — `instanceof PointerField` check and `pointerOverrides` sizing both update automatically via sealed hierarchy
- `skadistats.clarity.processor.sendtables.FieldGenerator` — routing logic for pointer field construction
- `skadistats.clarity.state.s2.S2EntityState` — `pointerSerializers` array shrinks
- Downstream: `clarity-analyzer` and any user code that pattern-matches on `PointerField` or `PointerDecoder` by class name
