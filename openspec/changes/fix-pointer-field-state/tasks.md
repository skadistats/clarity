## 1. PointerField Changes

- [x] 1.1 Make `PointerField.getChild()` null-safe: return null when `serializer` is null
- [x] 1.2 Similarly null-safe `getChildIndex()` and `getChildNameSegment()` for consistency
- [x] 1.3 Add `pointerId` field to `PointerField`, with setter and getter
- [x] 1.4 Add `getDefaultSerializer()` and `getSerializers()` accessors (already done)
- [x] 1.5 Remove `activateSerializer()` and `resetSerializer()` — PointerField becomes immutable
- [x] 1.6 Remove copy constructor (no longer needed — no cloning)

## 2. PointerId Assignment in FieldGenerator

- [x] 2.1 Add `nextPointerId` counter to `FieldGenerator`
- [x] 2.2 Assign `pointerId` to each PointerField when created in `createField()`
- [x] 2.3 Expose `getPointerCount()` on `FieldGenerator`

## 3. pointerCount Flow

- [x] 3.1 Add `pointerCount` field to `EntityStateFactory`, set from `S2DTClassEmitter`
- [x] 3.2 Pass `pointerCount` through `EntityStateFactory.forS2()` → `S2EntityStateType.createState()`
- [x] 3.3 Update `S2EntityStateType.createState()` signature to accept `pointerCount`

## 4. AbstractS2EntityState

- [x] 4.1 Create `AbstractS2EntityState` abstract class implementing `EntityState`
- [x] 4.2 Replace rootField cloning with `Serializer[] pointerSerializers` array (sized by `pointerCount`)
- [x] 4.3 Constructor takes `SerializerField` (shared, not cloned) + `pointerCount`
- [x] 4.4 Copy constructor: `pointerSerializers = other.pointerSerializers.clone()`
- [x] 4.5 Add `resolveChild(Field field, int idx)` — checks PointerField, looks up `pointerSerializers[pointerId]`, falls back to `defaultSerializer`
- [x] 4.6 Add `getPointerSerializer(int pointerId)` and `getPointerCount()` accessors
- [x] 4.7 Rewrite `getFieldForFieldPath(S2FieldPath)` to use `resolveChild`
- [x] 4.8 Rewrite `getNameForFieldPath(FieldPath)` to use `resolveChild`
- [x] 4.9 Rewrite `getFieldPathForName(String)` to use `resolveChild`
- [x] 4.10 `getTypeForFieldPath` / `getDecoderForFieldPath` — delegate to getFieldForFieldPath (already done)
- [x] 4.11 Remove `rootFieldOwned`, `ensureOwnRootField()`, `cloneWithOwnPointers()`, `validateNoNestedPointers()`
- [x] 4.12 Keep shared `SerializerField rootField` (not cloned) for Field-tree traversal

## 5. NestedArrayEntityState

- [x] 5.1 Extends `AbstractS2EntityState`
- [x] 5.2 Update constructor to pass `pointerCount` to super
- [x] 5.3 Update copy constructor — super handles pointerSerializers clone
- [x] 5.4 Rewrite `applyMutation` traversal to use `resolveChild` instead of `field.getChild()`
- [x] 5.5 Rewrite `handlePointerSwitch`: set `pointerSerializers[pf.getPointerId()]` instead of mutating the PointerField
- [x] 5.6 Rewrite `getValueForFieldPath` traversal to use `resolveChild`

## 6. TreeMapEntityState

- [x] 6.1 Extends `AbstractS2EntityState`
- [x] 6.2 Update constructor to pass `pointerCount` to super
- [x] 6.3 Update copy constructor
- [x] 6.4 Rewrite `applyMutation` SwitchPointer handling: set `pointerSerializers[pf.getPointerId()]`

## 7. S2EntityStateType and EntityStateFactory

- [x] 7.1 Update `createState` signature: `createState(SerializerField field, int pointerCount)`
- [x] 7.2 Update `EntityStateFactory.forS2()` to pass `pointerCount`

## 8. S2DTClass

- [x] 8.1 Remove navigation methods (already done)

## 9. DTClass Interface and S1DTClass

- [x] 9.1 Remove `getNameForFieldPath` / `getFieldPathForName` from `DTClass` interface (already done)
- [x] 9.2 Remove `@Override` annotations from `S1DTClass` (already done)

## 10. Entity Convenience Methods

- [x] 10.1–10.6 All done (navigation methods, hasProperty, getProperty, toString)

## 11. S2FieldReader

- [x] 11.1 Add `EntityState` parameter to `FieldReader.readFields()` interface
- [x] 11.2 Add `Serializer[] batchPointerOverrides` field — lazy allocated, reused across batches
- [x] 11.3 Implement `resolveChild` + `resolveField` — checks batchPointerOverrides by pointerId first, then state's pointerSerializers, then field.getChild() fallback
- [x] 11.4 Update `readFieldsFast`: resolve fields via `resolveField`; on SwitchPointer, store in `batchPointerOverrides[pf.getPointerId()]`; clear at batch end
- [x] 11.5 Update `readFieldsDebug`: same + use state's navigation for debug output
- [x] 11.6 Update `S1FieldReader.readFields()` to accept and ignore `EntityState` parameter

## 12. Entities.java Caller Updates

- [x] 12.1–12.5 All done (pass state to readFields, restructure create/recreate, baseline, TempEntities)

## 13. OnEntityPropertyChanged

- [x] 13.1–13.3 All done (Entity instead of DTClass)

## 14. Cleanup

- [x] 14.1 Remove `getFieldNames()` from `Serializer` (added for cloning, no longer needed)

## 15. External Projects

- [x] 15.1–15.5 All done (clarity-analyzer, clarity-examples, clarity-diff, dumpbaselines)

## 16. Verification

- [x] 16.1 Run `clarity-examples` entityrun against Deadlock replay — verify no errors
- [x] 16.2 Run `clarity-examples` entityrun against Dota 2 replay — verify no errors
- [x] 16.3 Run `clarity-examples` entityrun against CS2 replay — verify no errors
- [x] 16.4 Run `clarity-analyzer` — verify it builds and loads a replay without errors
