## 1. Rename ArrayEntityState to NestedEntityState

- [x] 1.1 Rename `ArrayEntityState` interface to `NestedEntityState`, update all method return types (`sub()`, `capacity()`) to return `NestedEntityState`
- [x] 1.2 Update `NestedArrayEntityState` to implement `NestedEntityState` instead of `ArrayEntityState`, including inner `Entry` class
- [x] 1.3 Update all Field classes to import and use `NestedEntityState` as parameter type

## 2. Rename Field methods

- [x] 2.1 Rename `Field.getArrayEntityState()` → `getValue()`, `setArrayEntityState()` → `setValue()`, `ensureArrayEntityStateCapacity()` → `ensureCapacity()` in base `Field` class
- [x] 2.2 Update method names in `ValueField`, `ArrayField`, `VectorField`, `PointerField`, `SerializerField`
- [x] 2.3 Update call sites in `NestedArrayEntityState.setValueForFieldPath()` and `getValueForFieldPath()`

## 3. Extract S2EntityState base class

- [x] 3.1 Create `S2EntityState` abstract class implementing `EntityState` and `NestedEntityState`, with `rootField` field and constructor
- [x] 3.2 Move traversal logic (`setValueForFieldPath`, `getValueForFieldPath`) from `NestedArrayEntityState` to `S2EntityState`, operating on `this` as root `NestedEntityState`
- [x] 3.3 Add abstract methods for `copy()`, `fieldPathIterator()`, and `capacityChanged()` tracking
- [x] 3.4 Make `NestedArrayEntityState` extend `S2EntityState`, remove duplicated traversal code, delegate root-level `NestedEntityState` methods to `rootEntry()`

## 4. TreeMapEntityState NestedEntityState implementation

- [x] 4.1 Make `TreeMapEntityState` extend `S2EntityState`, accept `SerializerField` in constructor
- [x] 4.2 Implement inner `View` class with `long prefix` using S2 long field path encoding, implementing `NestedEntityState`
- [x] 4.3 Implement `View.get()`, `set()`, `has()`, `clear()` via prefix + idx key construction on the shared map
- [x] 4.4 Implement `View.sub()` via `S2LongFieldPathFormat.down()`/`set()` to extend prefix
- [x] 4.5 Implement `View.length()` via sorted map range inspection under prefix
- [x] 4.6 Implement `View.capacity(n, shrink)` via sorted map range deletion for shrink, no-op for grow
- [x] 4.7 Implement `View.isSub()` by checking for keys deeper than prefix + idx
- [x] 4.8 Implement root-level `NestedEntityState` methods on `TreeMapEntityState` delegating to a root View

## 5. Update factory

- [x] 5.1 Update `S2EntityStateType` enum to pass `SerializerField` to `TreeMapEntityState` constructor
- [x] 5.2 Delete the `ArrayEntityState.java` file
- [x] 5.3 Build and verify compilation succeeds

## 6. Validate TreeMapEntityState correctness

- [ ] 6.1 Run a full replay parse with both `NESTED_ARRAY` and `TREE_MAP` state types, compare all entity property values at every tick to verify identical results
- [ ] 6.2 Specifically validate structural operations: vector resize (VectorField), pointer type switching (PointerField), capacity management (ArrayField/SerializerField) produce identical state
- [ ] 6.3 Verify `capacityChanged` tracking is consistent between both implementations (same return values from `setValueForFieldPath` for the same input sequence)
- [ ] 6.4 Verify `fieldPathIterator()` on TreeMapEntityState returns the same field paths as NestedArrayEntityState for the same entity state
- [ ] 6.5 Visual verification in clarity-analyzer: switch to TreeMap state type, browse entities, check that no unexpected warnings appear in `ObservableEntity.performUpdate` and that entity properties display correctly
