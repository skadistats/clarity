## 1. PointerField Changes

- [ ] 1.1 Add copy constructor to `PointerField` — copies immutable state, initializes mutable `serializer` from source
- [ ] 1.2 Make `PointerField.getChild()` null-safe: return null when `serializer` is null
- [ ] 1.3 Similarly null-safe `getChildIndex()` and `getChildNameSegment()` for consistency

## 2. Serializer Cloning Support

- [ ] 2.1 Add `getFieldNames()` (or equivalent) to `Serializer` to expose the fieldNames array for cloning
- [ ] 2.2 Ensure `Serializer` constructor accepts externally-provided `Field[]` and `String[]` arrays (already does)

## 3. AbstractS2EntityState

- [ ] 3.1 Create `AbstractS2EntityState` abstract class implementing `EntityState`
- [ ] 3.2 Implement rootField cloning in constructor: clone root Serializer's Field[], replace PointerFields with copies via copy constructor, set `rootFieldOwned = true`
- [ ] 3.3 Implement copy constructor with COW: share rootField from source, set `rootFieldOwned = false` on **both** original and copy
- [ ] 3.4 Implement `ensureOwnRootField()`: if `!rootFieldOwned`, clone rootField and set flag to true. Called before any PointerField mutation
- [ ] 3.4 Move `getFieldForFieldPath(S2FieldPath)` from S2DTClass — same body, `field` → `rootField`
- [ ] 3.5 Move `getNameForFieldPath(FieldPath)` from S2DTClass — same body, `field` → `rootField`
- [ ] 3.6 Move `getFieldPathForName(String)` from S2DTClass — same body, `field` → `rootField`
- [ ] 3.7 Move `getTypeForFieldPath(S2FieldPath)` from S2DTClass — delegates to getFieldForFieldPath
- [ ] 3.8 Move `getDecoderForFieldPath(S2FieldPath)` from S2DTClass — delegates to getFieldForFieldPath
- [ ] 3.9 Expose rootField to subclasses (protected) for traversal in applyMutation etc.

## 4. NestedArrayEntityState

- [ ] 4.1 Change `NestedArrayEntityState` to extend `AbstractS2EntityState` instead of implementing `EntityState` directly
- [ ] 4.2 Update constructor to pass `SerializerField` to super
- [ ] 4.3 Update copy constructor to pass `other` to super (for rootField cloning)
- [ ] 4.4 Remove `rootField` field (now inherited from AbstractS2EntityState)
- [ ] 4.5 Verify `handlePointerSwitch` works correctly — calls `pf.activateSerializer()` on own copy, no changes needed
- [ ] 4.6 Verify `applyMutation` traversal works correctly — `field.getChild()` uses own PointerField, no changes needed
- [ ] 4.7 Verify `getValueForFieldPath` traversal works correctly — same reason, no changes needed

## 5. TreeMapEntityState

- [ ] 5.1 Change `TreeMapEntityState` to extend `AbstractS2EntityState`
- [ ] 5.2 Update constructor to accept `SerializerField` and pass to super
- [ ] 5.3 Update copy constructor to pass `other` to super
- [ ] 5.4 Update `S2EntityStateType.TREE_MAP.createState()` to pass the `SerializerField` to constructor
- [ ] 5.5 Update `applyMutation` SwitchPointer handling: activate new serializer on own PointerField copy (in addition to existing `clearSubEntries`)
- [ ] 5.6 Update `applyMutation` SwitchPointer handling: need to resolve the PointerField from rootField at the given FieldPath position

## 6. S2DTClass

- [ ] 6.1 Remove `getFieldForFieldPath(S2FieldPath)`
- [ ] 6.2 Remove `getNameForFieldPath(FieldPath)` (was `@Override`)
- [ ] 6.3 Remove `getFieldPathForName(String)` (was `@Override`)
- [ ] 6.4 Remove `getTypeForFieldPath(S2FieldPath)`
- [ ] 6.5 Remove `getDecoderForFieldPath(S2FieldPath)`

## 7. DTClass Interface and S1DTClass

- [ ] 7.1 Remove `getNameForFieldPath(FieldPath)` from `DTClass` interface
- [ ] 7.2 Remove `getFieldPathForName(String)` from `DTClass` interface
- [ ] 7.3 Remove `@Override` annotations from `S1DTClass.getNameForFieldPath()` and `getFieldPathForName()`

## 8. Entity Convenience Methods

- [ ] 8.1 Add `Entity.getNameForFieldPath(FieldPath)` — dispatches via `dtClass.evaluate()` to S1DTClass or AbstractS2EntityState
- [ ] 8.2 Add `Entity.getFieldPathForName(String)` — same dispatch pattern
- [ ] 8.3 Add `Entity.getFieldForFieldPath(FieldPath)` — S2 only, delegates to AbstractS2EntityState
- [ ] 8.4 Update `Entity.hasProperty()` to use `this.getFieldPathForName()`
- [ ] 8.5 Update `Entity.getProperty()` to use `this.getFieldPathForName()`
- [ ] 8.6 Update `Entity.toString()` to use `this::getNameForFieldPath` as name resolver

## 9. S2FieldReader

- [ ] 9.1 Add `EntityState` parameter to `FieldReader.readFields()` interface
- [ ] 9.2 Add local `batchPointerOverrides` (Serializer[]) field to S2FieldReader, null between batches
- [ ] 9.3 Add private `resolveField(AbstractS2EntityState, S2FieldPath)` — uses state's rootField for normal fields, checks `batchPointerOverrides` at root level for pointer fields
- [ ] 9.4 Update `readFieldsFast`: use `resolveField()` for field resolution; on SwitchPointer mutation, store new serializer in `batchPointerOverrides`; null the array at end of batch
- [ ] 9.5 Update `readFieldsDebug`: same changes + use state's `getNameForFieldPath()`, `getTypeForFieldPath()` for debug output
- [ ] 9.6 Critical: FieldReader MUST NOT mutate the state during decode — pointer mutations are deferred to `FieldChanges.applyTo()`
- [ ] 9.7 Update `S1FieldReader.readFields()` to accept and ignore `EntityState` parameter

## 10. Entities.java Caller Updates

- [ ] 10.1 Update `queueEntityUpdate`: pass `entity.getState()` to `readFields`
- [ ] 10.2 Restructure `queueEntityCreate`: get baseline state before `readFields`, pass baseline state
- [ ] 10.3 Update `queueEntityRecreate`: pass baseline state to `readFields`
- [ ] 10.4 Update baseline parse in `getBaseline`: pass the new empty state to `readFields`
- [ ] 10.5 Update `TempEntities` if it calls `readFields`

## 11. OnEntityPropertyChanged

- [ ] 11.1 Change `Adapter.propertyMatches` signature from `(DTClass, FieldPath)` to `(Entity, FieldPath)`
- [ ] 11.2 Use `entity.getNameForFieldPath(fp)` instead of `dtClass.getNameForFieldPath(fp)`
- [ ] 11.3 Update `raise()` to pass entity instead of dtClass to `propertyMatches`

## 12. FieldGenerator Validation

- [ ] 12.1 Throw `ClarityException` if `PointerField` is created inside a sub-serializer or vector element (non-root level)

## 13. External Projects

- [ ] 13.1 Update `clarity-analyzer` `ObservableEntity.java`: switch from `dtClass.getXxx()` to `entity.getXxx()`
- [ ] 13.2 Update `clarity-analyzer` PositionBinders: switch to `entity.getXxx()`
- [ ] 13.3 Update `clarity-examples` affected examples: `resources`, `matchend`, `propertychange`, `position`, `cooldowns`, `dumpmana`, `lifestate`

## 14. Verification

- [ ] 14.1 Run `clarity-examples` entityrun against Deadlock replay — verify no errors
- [ ] 14.2 Run `clarity-examples` entityrun against Dota 2 replay — verify no errors
- [ ] 14.3 Run `clarity-examples` entityrun against CS2 replay — verify no errors
- [ ] 14.4 Run `clarity-analyzer` — verify it builds and loads a replay without errors
- [ ] 14.5 Run `clarity-examples` dtinspector against Deadlock replay — verify pointer navigation works
