## MODIFIED Requirements

### Requirement: S2FieldReader uses switch dispatch for field ops

`S2FieldReader.readFields()` SHALL execute field operations via a `switch` statement on the int ordinal returned by `readFieldOpId()`, with each case containing the operation body directly (no virtual method call). The switch SHALL cover all 40 field op types.

Each case SHALL mutate a `S2FieldPathBuilder` obtained from the configured `S2FieldPathType` — NOT a `S2ModifiableFieldPath` or any `S2FieldPath` subtype. When the reader needs to emit an immutable path (for the `fieldPaths[]` array or as a map key), it SHALL call `builder.snapshot()`.

#### Scenario: Switch dispatch produces identical parse output

- **WHEN** a replay is parsed using the switch-based dispatch
- **THEN** the resulting `FieldChanges` SHALL be bit-exact identical to the output produced by the previous virtual-dispatch implementation for all field op sequences

#### Scenario: FieldPathEncodeFinish terminates the loop

- **WHEN** `readFieldOpId()` returns `OP_FIELD_PATH_ENCODE_FINISH`
- **THEN** the field-op loop SHALL exit and proceed to the decoder phase without storing a field path entry

#### Scenario: Builder is obtained from S2FieldPathType

- **WHEN** `readFields` begins a field-op sequence
- **THEN** the builder used by the switch cases is produced by the configured `S2FieldPathType.newBuilder()` call path
- **AND** each stored field path in the `fieldPaths[]` array is the result of `builder.snapshot()`
- **AND** the stored paths are immutable and independent of subsequent builder mutations

### Requirement: Fast and debug paths are separated

`readFields()` SHALL delegate to a dedicated fast-path method (no debug instrumentation) when debug mode is inactive, and to a separate debug-path method when debug mode is active. Both paths SHALL produce identical `FieldChanges` output. Both SHALL operate on the same `S2FieldPathBuilder` interface — neither SHALL refer to a concrete builder implementor.

#### Scenario: Fast path has no debug overhead

- **WHEN** `readFields()` is called with `debug = false`
- **THEN** the fast-path method SHALL be invoked, containing no TextTable operations, no string formatting, and no per-op position tracking
- **AND** its builder parameter is typed `S2FieldPathBuilder`

#### Scenario: Debug path retains full instrumentation

- **WHEN** `readFields()` is called with `debug = true`
- **THEN** the debug-path method SHALL produce identical TextTable output to the current implementation
- **AND** its builder parameter is typed `S2FieldPathBuilder`
