## ADDED Requirements

### Requirement: S2NestedArrayEntityState accepts S2FieldPath without concrete-type casts

`S2NestedArrayEntityState` SHALL accept `S2FieldPath` at all public entry points (`write`, `applyMutation`, `getValueForFieldPath`). Its Field+Entry traversal SHALL use only the interface methods `fp.get(int)` and `fp.last()` to navigate. No `instanceof` checks against concrete `S2FieldPath` implementors and no reference to `S2LongFieldPathFormat` (or equivalent encoding utilities) SHALL appear.

#### Scenario: Path walk uses only interface methods

- **WHEN** `S2NestedArrayEntityState.write(fp, decoded)` traverses the Field+Entry hierarchy
- **THEN** the traversal indexes with `fp.get(depth)` up through `fp.last()`
- **AND** no concrete-path cast or encoding utility appears

#### Scenario: All S2FieldPath impls work uniformly

- **WHEN** `S2NestedArrayEntityState` is exercised with a `S2FieldPath` from any concrete implementor
- **THEN** observable behaviour is identical across implementors for equivalent paths
- **AND** stored/retrieved values match bit-for-bit
