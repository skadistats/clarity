## Purpose

TreeMapEntityState implementation of NestedEntityState using S2 long-encoded prefix views for sorted-map-based entity state storage.

## Requirements

### Requirement: TreeMapEntityState extends S2EntityState
`TreeMapEntityState` SHALL extend `S2EntityState` and implement `NestedEntityState` methods. It SHALL accept a `SerializerField` in its constructor (passed to the base class). It SHALL use its existing `Object2ObjectAVLTreeMap<FieldPath, Object>` for storage, where keys are `S2LongFieldPath` instances.

#### Scenario: Construction with SerializerField
- **WHEN** a `TreeMapEntityState` is created via `S2EntityStateType.TREE_MAP`
- **THEN** the `SerializerField` is passed to the `S2EntityState` base class and the field-driven traversal logic is available

### Requirement: capacityChanged tracking
`TreeMapEntityState` SHALL track whether structural changes occurred during a `setValueForFieldPath` call, consistent with the `S2EntityState.capacityChanged()` contract. A structural change is any insertion of a new key or removal of an existing key.

#### Scenario: New key inserted
- **WHEN** `set(idx, value)` is called on a View and no key with prefix + idx existed
- **THEN** `capacityChanged()` SHALL return true for the current traversal

#### Scenario: Key removed
- **WHEN** `clear(idx)` or `capacity(n, true)` removes keys from the map
- **THEN** `capacityChanged()` SHALL return true for the current traversal

#### Scenario: Value overwritten
- **WHEN** `set(idx, value)` is called on a View and a key with prefix + idx already existed
- **THEN** `capacityChanged()` SHALL NOT be set by this operation alone

### Requirement: TreeMapEntityState root-level NestedEntityState operations
The root `TreeMapEntityState` SHALL implement `NestedEntityState` methods by delegating to a root-level view with an empty prefix.

#### Scenario: Root get and set
- **WHEN** `get(idx)` or `set(idx, value)` is called on the root TreeMapEntityState
- **THEN** the operation maps to the underlying tree map using a FieldPath constructed from the single index

### Requirement: Prefix view implementation
`TreeMapEntityState` SHALL provide an inner `View` class implementing `NestedEntityState`. A View SHALL store a reference to the root `TreeMapEntityState` and a `long prefix` using S2 long field path encoding.

#### Scenario: sub() returns a prefix view
- **WHEN** `sub(idx)` is called on a TreeMapEntityState or View
- **THEN** a new `View` is returned with the prefix extended by `idx` using `S2LongFieldPathFormat.down()` and `set()`

#### Scenario: View get and set
- **WHEN** `get(idx)` or `set(idx, value)` is called on a View
- **THEN** the operation constructs an `S2LongFieldPath` key from prefix + idx and operates on the shared tree map

### Requirement: Prefix view capacity and length
The View SHALL support `capacity(n, shrinkIfNeeded)` using sorted map range operations, and `length()` by inspecting the key range under its prefix.

#### Scenario: capacity with shrink
- **WHEN** `capacity(n, true)` is called on a View
- **THEN** all map entries whose key is under the view's prefix with an index >= n SHALL be removed

#### Scenario: capacity without shrink (grow)
- **WHEN** `capacity(n, false)` is called on a View and n > current length
- **THEN** no map entries are created (TreeMap does not need pre-allocation)

#### Scenario: length
- **WHEN** `length()` is called on a View
- **THEN** the result is derived from the highest index present in the map under the view's prefix, plus one

### Requirement: Prefix view has, clear, isSub
The View SHALL support `has(idx)`, `clear(idx)`, and `isSub(idx)` using the tree map.

#### Scenario: has checks key existence
- **WHEN** `has(idx)` is called on a View
- **THEN** it returns true if any key exists in the map that starts with prefix + idx (either an exact leaf match or deeper nested keys)

#### Scenario: clear removes subtree
- **WHEN** `clear(idx)` is called on a View
- **THEN** all map entries whose key starts with prefix + idx SHALL be removed

#### Scenario: isSub checks for nested entries
- **WHEN** `isSub(idx)` is called on a View
- **THEN** it returns true if there exist keys in the map that extend beyond prefix + idx (i.e., have deeper components)
