## 1. Inline apply in readFieldsMaterialized

- [x] 1.1 Replace the collect-then-apply loop with an inline decode → apply → notify loop, accumulating `capacityChanged` from each write
- [x] 1.2 Remove `Arrays.fill(pointerOverrides, null)` call and the `SwitchPolymorphicPointer` override-tracking block from `readFieldsMaterialized`
- [x] 1.3 Switch `readFieldsMaterialized` to call `resolveField` instead of `resolveFieldDebug`

## 2. Inline apply in readFieldsDebug

- [x] 2.1 Apply the same inline decode → apply → notify pattern in `readFieldsDebug`
- [x] 2.2 Remove `Arrays.fill(pointerOverrides, null)` and the `SwitchPolymorphicPointer` tracking block from `readFieldsDebug`
- [x] 2.3 Switch `readFieldsDebug` to call `resolveField` instead of `resolveFieldDebug`

## 3. Remove dead code

- [x] 3.1 Delete `resolveFieldDebug` method from `S2FieldReader`
- [x] 3.2 Delete `pointerOverrides` field and its initialization in the constructor
- [x] 3.3 Remove `PolymorphicPointerField` import from `S2FieldReader` if no longer referenced
- [x] 3.4 Check `FieldChanges.applyTo(state, callback)` — remove overload if no longer called from `Entities` or elsewhere

## 4. Fix FieldChanges capacityChanged for materialize path

- [x] 4.1 Update the two-arg `FieldChanges` constructor (or add a setter) so `readFieldsMaterialized` can return the correct `capacityChanged` flag

## 5. Verify

- [x] 5.1 Build clarity and clarity-examples clean (`./gradlew build` in both)
- [x] 5.2 Build clarity-analyzer to confirm no API breakage
- [x] 5.3 Run a full replay parse with a `MutationListener` attached and verify trace output is unchanged
