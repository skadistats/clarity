## MODIFIED Requirements

### Requirement: S2TreeMapEntityState implements EntityState directly
`S2TreeMapEntityState` SHALL implement `EntityState` directly. It SHALL NOT extend `S2EntityState` or implement `S2NestedEntityState`. It SHALL use an `Object2ObjectAVLTreeMap<S2FieldPath, Object>` for storage, keyed on the sealed `S2FieldPath` interface — NOT on any concrete implementor. The map relies on `S2FieldPath`'s `Comparable<S2FieldPath>` contract for ordering; no concrete-type cast SHALL appear at the `write` / `applyMutation` boundary.

Range operations (sub-entry clear, vector trim) SHALL be expressed via interface methods on `S2FieldPath` (`childAt`, `upperBoundForSubtreeAt`). `S2TreeMapEntityState` SHALL NOT reference any concrete path encoding utility (e.g., `S2LongFieldPathFormat`) directly.

#### Scenario: Construction
- **WHEN** a `S2TreeMapEntityState` is created via `S2EntityStateType.TREE_MAP`
- **THEN** no S2EntityState base class traversal logic is involved
- **AND** the internal map is typed `Object2ObjectAVLTreeMap<S2FieldPath, Object>`

#### Scenario: applyMutation with WriteValue
- **WHEN** `applyMutation(fp, WriteValue(value))` is called
- **THEN** the value is stored directly via `state.put(fp, value)` with no cast
- **AND** returns true if a new key was inserted (capacity changed)

#### Scenario: applyMutation with ResizeVector
- **WHEN** `applyMutation(fp, ResizeVector(count))` is called
- **THEN** the `from`/`to` bounds for the range are built via `fp.childAt(count)` and `fp.upperBoundForSubtreeAt(fp.last())`
- **AND** all entries in `state.subMap(from, to)` are removed
- **AND** the ResizeVector value itself is NOT stored in the map
- **AND** returns true if any entries were removed

#### Scenario: applyMutation with SwitchPointer
- **WHEN** `applyMutation(fp, SwitchPointer(newSerializer))` is called
- **AND** newSerializer differs from the current pointer state
- **THEN** the sub-range of `state` strictly under `fp` is cleared using interface-method bound construction
- **AND** the SwitchPointer value itself is NOT stored in the map
- **WHEN** newSerializer is null
- **THEN** the same sub-range is cleared

#### Scenario: getValueForFieldPath
- **WHEN** `getValueForFieldPath(fp)` is called
- **THEN** it returns `state.get(fp)` directly with no cast — no traversal needed

#### Scenario: No concrete-type leakage
- **WHEN** inspecting the source of `S2TreeMapEntityState`
- **THEN** no reference to `S2LongFieldPath` appears outside type-narrowing that is strictly internal (e.g., generic diamond on construction)
- **AND** no reference to `S2LongFieldPathFormat` appears at all
