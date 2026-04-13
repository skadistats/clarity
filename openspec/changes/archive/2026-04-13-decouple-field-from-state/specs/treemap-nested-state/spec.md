## MODIFIED Requirements

### Requirement: TreeMapEntityState extends S2EntityState
`TreeMapEntityState` SHALL implement `EntityState` directly. It SHALL NOT extend `S2EntityState` or implement `NestedEntityState`. It SHALL accept a `SerializerField` in its constructor for field path resolution. It SHALL use its existing `Object2ObjectAVLTreeMap<S2LongFieldPath, Object>` for storage.

#### Scenario: Construction with SerializerField
- **WHEN** a `TreeMapEntityState` is created via `S2EntityStateType.TREE_MAP`
- **THEN** the `SerializerField` is stored for field path resolution
- **AND** no S2EntityState base class traversal logic is involved

#### Scenario: applyMutation with WriteValue
- **WHEN** `applyMutation(fp, WriteValue(value))` is called
- **THEN** the value is stored directly via `state.put(fp.s2(), value)`
- **AND** returns true if a new key was inserted (capacity changed)

#### Scenario: applyMutation with ResizeVector
- **WHEN** `applyMutation(fp, ResizeVector(count))` is called
- **THEN** all entries with keys under the FieldPath prefix whose index >= count are removed via subMap range operations
- **AND** the ResizeVector value itself is NOT stored in the map
- **AND** returns true if any entries were removed

#### Scenario: applyMutation with SwitchPointer
- **WHEN** `applyMutation(fp, SwitchPointer(newSerializer))` is called
- **AND** newSerializer differs from the current pointer state
- **THEN** all entries under the FieldPath prefix are cleared
- **AND** the SwitchPointer value itself is NOT stored in the map
- **WHEN** newSerializer is null
- **THEN** all entries under the FieldPath prefix are cleared

#### Scenario: getValueForFieldPath
- **WHEN** `getValueForFieldPath(fp)` is called
- **THEN** it returns `state.get(fp.s2())` directly — no traversal needed

### Requirement: capacityChanged tracking
`TreeMapEntityState` SHALL track whether structural changes occurred during an `applyMutation` call. A structural change is any insertion of a new key or removal of an existing key.

#### Scenario: New key inserted
- **WHEN** `applyMutation` processes a `WriteValue` and the key did not previously exist in the map
- **THEN** `applyMutation` returns true

#### Scenario: Key removed
- **WHEN** `applyMutation` processes a `ResizeVector` or `SwitchPointer` that removes entries
- **THEN** `applyMutation` returns true

#### Scenario: Value overwritten
- **WHEN** `applyMutation` processes a `WriteValue` and the key already existed
- **THEN** `applyMutation` returns false

## REMOVED Requirements

### Requirement: TreeMapEntityState root-level NestedEntityState operations
**Reason**: TreeMapEntityState no longer implements NestedEntityState. State operations go through `applyMutation(FieldPath, StateMutation)` directly.
**Migration**: Use `applyMutation` for writes and `getValueForFieldPath` for reads.

### Requirement: Prefix view implementation
**Reason**: The View inner class is removed. TreeMapEntityState operates directly on the TreeMap via FieldPath keys and subMap range operations. No nested state navigation needed.
**Migration**: Structural operations (vector resize, pointer switch) are handled via StateMutation dispatch in `applyMutation`, using subMap range operations for cleanup.

### Requirement: Prefix view capacity and length
**Reason**: Removed with the View class. Capacity management (vector resize) is handled by `ResizeVector` StateMutation via subMap range operations.
**Migration**: Vector resize uses `ResizeVector` StateMutation. Length is implicit in the map key range.

### Requirement: Prefix view has, clear, isSub
**Reason**: Removed with the View class. Entry existence and cleanup are handled via direct TreeMap operations in `applyMutation`.
**Migration**: Sub-entry cleanup uses subMap range operations in the `SwitchPointer` handler.
