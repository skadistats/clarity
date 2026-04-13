## 1. StateMutation Interface

- [x] 1.1 Create `StateMutation` sealed interface with records `WriteValue(Object value)`, `ResizeVector(int count)`, `SwitchPointer(Serializer newSerializer)` in `skadistats.clarity.model.state`

## 2. Field.createMutation

- [x] 2.1 Add default `createMutation(Object decodedValue)` method to `Field` returning `WriteValue(decodedValue)`
- [x] 2.2 Override `createMutation` in `VectorField` to validate count and return `ResizeVector(count)`
- [x] 2.3 Override `createMutation` in `PointerField` to resolve Serializer and return `SwitchPointer(newSerializer)`

## 3. FieldChanges + FieldReader

- [x] 3.1 Change `FieldChanges` from `Object[] values` to `StateMutation[] mutations`, rename `getValue`/`setValue` to `getMutation`/`setMutation`
- [x] 3.2 Update `FieldChanges.applyTo` to call `state.applyMutation(fp, mutation)` instead of `state.setValueForFieldPath(fp, value)`
- [x] 3.3 Update `S2FieldReader.readFieldsFast` to use `dtClass.getFieldForFieldPath(fp)`, `field.getDecoder()`, and `field.createMutation(decoded)`
- [x] 3.4 Update `S2FieldReader.readFieldsDebug` to work with StateMutation (extract display value from WriteValue for debug output)
- [x] 3.5 Update `S1FieldReader` to wrap decoded values in `WriteValue`

## 4. EntityState Interface

- [x] 4.1 Replace `setValueForFieldPath(FieldPath, Object)` with `applyMutation(FieldPath, StateMutation)` on `EntityState` interface
- [x] 4.2 Update `ObjectArrayEntityState` (S1) to implement `applyMutation`

## 5. NestedArrayEntityState — Own Traversal

- [x] 5.1 Move `setValueForFieldPath` traversal from `S2EntityState` into `NestedArrayEntityState.applyMutation`, replacing `field.setValue` call with `switch(mutation)` dispatch at the leaf
- [x] 5.2 Implement `ensureNodeCapacity` using Field structural info (SerializerField → fieldCount, ArrayField → length) instead of `field.ensureCapacity`
- [x] 5.3 Implement `handlePointerSwitch` inline (logic from PointerField.setValue)
- [x] 5.4 Move `getValueForFieldPath` traversal from `S2EntityState` into `NestedArrayEntityState`, reading from Entry directly without `field.getValue`

## 6. TreeMapEntityState — Direct Operations

- [x] 6.1 Implement `applyMutation` with direct `switch(mutation)` dispatch: `WriteValue` → put, `ResizeVector` → trimEntries, `SwitchPointer` → clearSubEntries
- [x] 6.2 Implement `getValueForFieldPath` as direct `state.get(fp.s2())`
- [x] 6.3 Remove `View` inner class and all `NestedEntityState` method implementations
- [x] 6.4 Remove `S2EntityState` base class reference, implement `EntityState` directly

## 7. Cleanup

- [x] 7.1 Remove `S2EntityState` class
- [x] 7.2 Remove `setValue`, `getValue`, `ensureCapacity`, `isHiddenFieldPath` from `Field` and all subclasses (ValueField, VectorField, PointerField, SerializerField, ArrayField)
- [x] 7.3 Remove `NestedEntityState` import from all Field classes
- [x] 7.4 Reduce `NestedEntityState` scope — ensure it's only referenced by `NestedArrayEntityState` and its Entry class

## 8. Verification

- [x] 8.1 Run all existing examples with NestedArrayEntityState, verify identical output
- [x] 8.2 Run all existing examples with TreeMapEntityState, verify identical output
- [x] 8.3 Run existing tests, verify all pass
