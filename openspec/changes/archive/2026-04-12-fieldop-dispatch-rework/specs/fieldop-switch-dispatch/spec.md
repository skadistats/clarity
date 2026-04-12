## ADDED Requirements

### Requirement: BitStream provides int-returning field op reader

`BitStream` SHALL provide a `readFieldOpId()` method that returns the field op ordinal as a primitive `int`, without converting to a `FieldOpType` enum constant. The existing `readFieldOp()` method SHALL remain available for debug and backward-compatible use.

#### Scenario: readFieldOpId returns correct ordinal
- **WHEN** a BitStream contains the Huffman encoding for `PlusOne` (ordinal 0)
- **THEN** `readFieldOpId()` SHALL return `0` and advance the bit position identically to `readFieldOp()`

#### Scenario: readFieldOpId handles slow-path Huffman codes
- **WHEN** a BitStream contains a Huffman code that requires more than 8 bits (slow path)
- **THEN** `readFieldOpId()` SHALL return the correct ordinal and advance the bit position identically to `readFieldOp()`

### Requirement: FieldOpType exposes ordinal constants

`FieldOpType` SHALL expose `public static final int` constants for each field op ordinal (e.g., `OP_PLUS_ONE`, `OP_PLUS_TWO`, ..., `OP_FIELD_PATH_ENCODE_FINISH`). These constants SHALL be derived from `ordinal()` at class initialization time, ensuring they remain synchronized with enum declaration order.

#### Scenario: Constants match enum ordinals
- **WHEN** `FieldOpType` is loaded
- **THEN** each `OP_*` constant SHALL equal the `ordinal()` of its corresponding enum constant (e.g., `OP_PLUS_ONE == PlusOne.ordinal()`)

### Requirement: S2FieldReader uses switch dispatch for field ops

`S2FieldReader.readFields()` SHALL execute field operations via a `switch` statement on the int ordinal returned by `readFieldOpId()`, with each case containing the operation body directly (no virtual method call). The switch SHALL cover all 40 field op types.

#### Scenario: Switch dispatch produces identical parse output
- **WHEN** a replay is parsed using the switch-based dispatch
- **THEN** the resulting `FieldChanges` SHALL be bit-exact identical to the output produced by the previous virtual-dispatch implementation for all field op sequences

#### Scenario: FieldPathEncodeFinish terminates the loop
- **WHEN** `readFieldOpId()` returns `OP_FIELD_PATH_ENCODE_FINISH`
- **THEN** the field-op loop SHALL exit and proceed to the decoder phase without storing a field path entry

### Requirement: Fast and debug paths are separated

`readFields()` SHALL delegate to a dedicated fast-path method (no debug instrumentation) when debug mode is inactive, and to a separate debug-path method when debug mode is active. Both paths SHALL produce identical `FieldChanges` output.

#### Scenario: Fast path has no debug overhead
- **WHEN** `readFields()` is called with `debug = false`
- **THEN** the fast-path method SHALL be invoked, containing no TextTable operations, no string formatting, and no per-op position tracking

#### Scenario: Debug path retains full instrumentation
- **WHEN** `readFields()` is called with `debug = true`
- **THEN** the debug-path method SHALL produce identical TextTable output to the current implementation
