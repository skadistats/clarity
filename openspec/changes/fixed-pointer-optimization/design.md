## Context

Pointer fields in the S2 sendtable schema come in two kinds: those with a single possible serializer (fixed) and those with multiple possible serializers (polymorphic). Currently both are handled by the same `PointerField` class, which always allocates a `pointerSerializers` slot in every `S2EntityState` instance and performs an array lookup on every child navigation. For fixed pointer fields the slot is never meaningfully written (the decoder never emits a type index) and the lookup always falls through to `defaultSerializer`. A scan across all benchmark replays shows 8–23 fixed pointer fields per game and exactly 1 truly polymorphic field (CS2 only).

## Goals / Non-Goals

**Goals:**
- Eliminate `pointerSerializers` slot waste for fixed pointer fields
- Remove the `types.length > 1` branch from the polymorphic decoder hot path
- Make the sealed `Field` hierarchy self-documenting about which pointer kinds exist

**Non-Goals:**
- Changing the wire format consumed by either pointer kind
- Optimizing entity state storage beyond pointer serializer slots
- Supporting concurrent multi-threaded parsing (not a current requirement)

## Decisions

### Separate classes over a conditional branch in `PointerField`

Two new concrete classes (`FixedPointerField`, `PolymorphicPointerField`) rather than an `isSingleType` flag inside the existing class.

**Rationale**: The sealed `Field` hierarchy already uses one class per structural variant. Two classes let the JIT monomorphize `getChild`/`resolveSerializer` call sites independently. A flag would add a branch to every call on the existing class without eliminating the `pointerId`/`pointerSerializers` machinery.

**Alternative considered**: Keep one class, skip `pointerId` assignment when `serializers.length == 1`. Rejected — `resolveSerializer` would still need a null-check branch and the slot reduction only works if the field is truly never registered.

### `FixedPointerDecoder` returns `Boolean`

The decoder reads one bit (presence flag) and returns `Boolean`.

**Rationale**: The decoded value is unused by `prepareForWrite` and `createMutation` (both return null for fixed fields), but surfaces in the debug table (`dataDebugTable.setData(r, 8, decoded)`). Returning `Boolean` gives meaningful output there. Returning null would require a null-check in debug rendering.

### `PolymorphicPointerDecoder` removes `types.length > 1` guard

The decoder always reads the `ubitvar` type index after a `true` presence bit.

**Rationale**: `PolymorphicPointerDecoder` is only ever instantiated for fields with `types.length > 1` (enforced at construction in `FieldGenerator`). The guard is redundant by construction. Removing it eliminates a branch from the hot path.

### `createMutation()` returns null for `FixedPointerField`; no new mutation type

**Rationale**: In `readFieldsMaterialized` and `readFieldsDebug`, the post-decode check is `mutation instanceof StateMutation.SwitchPointer` — `null instanceof X` is false, so the block is safely skipped. No new `StateMutation.Noop` type is needed. In `readFieldsFast`, `prepareForWrite` returns null and `state.write(fp, null)` is already a valid no-op path.

### Rename `PointerField` → `PolymorphicPointerField`, `PointerDecoder` → `PolymorphicPointerDecoder`

**Rationale**: Names should distinguish the two variants. Leaving the existing name on the polymorphic class would be confusing alongside `FixedPointerField`. This is a breaking rename for any downstream code referencing these classes directly.

## Risks / Trade-offs

- **Breaking rename** → Downstream (clarity-analyzer, user code) referencing `PointerField` or `PointerDecoder` by class name will fail to compile. Mitigation: change targets the `next` branch (5.x), not a patch release. Document in release notes.
- **`S2FieldReader` instanceof check** → Line 85 checks `instanceof PointerField` for the `pointerOverrides` path. After rename this becomes `instanceof PolymorphicPointerField`; fixed pointer fields fall through to the generic `getChild` path, which is correct. Risk: if any other code does an unchecked cast to `PointerField`, it will fail at runtime for fixed fields. Mitigation: search the codebase for all `PointerField` references before releasing.

## Open Questions

(none)
