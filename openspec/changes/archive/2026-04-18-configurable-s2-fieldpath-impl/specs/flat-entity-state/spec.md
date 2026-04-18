## ADDED Requirements

### Requirement: S2FlatEntityState accepts S2FieldPath without concrete-type casts

`S2FlatEntityState` SHALL accept `S2FieldPath` at all public entry points (`write`, `decodeInto`, `applyMutation`, `getValueForFieldPath`). It SHALL walk the path using only the interface methods (`get(int)`, `last()`) — NOT by downcasting to a concrete implementor or by reading any impl-specific encoding.

No reference to `S2LongFieldPath`, `S2LongFieldPathFormat`, or any other concrete path utility SHALL appear in `S2FlatEntityState`.

#### Scenario: Path walk uses only interface methods

- **WHEN** `S2FlatEntityState.write(fp, value)` walks the `FieldLayout` tree to reach the target slot
- **THEN** each step consults `fp.get(depth)` and `fp.last()` only
- **AND** no `instanceof S2LongFieldPath` or equivalent check appears

#### Scenario: All S2FieldPath impls work uniformly

- **WHEN** `S2FlatEntityState` receives a `S2FieldPath` from a different concrete implementor than `S2LongFieldPath` (e.g. a future interned impl)
- **THEN** behaviour is unchanged — the traversal succeeds using the interface contract
- **AND** stored/retrieved values are bit-exact identical to those under the LONG impl for equivalent paths
