## Purpose

Make the S2 field-path representation a runner-level configuration knob. `S2FieldPath` becomes an immutable, comparable key contract whose concrete implementation is selected at parse startup via an `S2FieldPathType` enum. Mutation during field-op decoding is moved to a separate `S2FieldPathBuilder` concept that is NOT a subtype of `S2FieldPath`, eliminating the inverted-lifecycle sibling relationship that previously forced concrete-type casts in state impls and leaked long-encoding details into impl-agnostic code. This opens the door to experimenting with alternative path encodings (interned, array-backed, packed-short) without touching every state impl, and adding a new impl requires exactly one enum entry, one path implementor, and one builder implementor.

## Requirements

### Requirement: S2FieldPath is a sealed immutable key contract

`S2FieldPath` SHALL be a `sealed` interface whose permitted implementors are all immutable, comparable, and safe to use as map keys. The sealing SHALL NOT include any mutable-builder type. Every `S2FieldPath` implementor SHALL implement `Comparable<S2FieldPath>` with ordering that matches the lexicographic path order (by `get(0), get(1), …, get(last())`).

Each `S2FieldPath` implementor SHALL additionally provide:

- `S2FieldPath childAt(int index)` — returns a path that extends `this` by one level, with `index` as the value at the new depth.
- `S2FieldPath upperBoundForSubtreeAt(int depth)` — returns a path that sorts strictly greater than any descendant of `this` at the given depth; used as an exclusive upper bound in `subMap`-style range queries.

#### Scenario: Sealing excludes mutable types

- **WHEN** the compiler enumerates permitted subtypes of `S2FieldPath`
- **THEN** only immutable, comparable implementors appear (e.g. `S2LongFieldPath`)
- **AND** no subtype of `S2FieldPath` supports in-place mutation

#### Scenario: Comparable matches lexicographic path order

- **GIVEN** two `S2FieldPath` instances `a` and `b` of the same implementor
- **WHEN** they differ at the smallest depth `d` and `a.get(d) < b.get(d)`
- **THEN** `a.compareTo(b) < 0`
- **AND** `a.compareTo(b) > 0` when the inequality reverses
- **AND** `a.compareTo(b) == 0` iff `a.equals(b)`

#### Scenario: Range-op methods compose correctly

- **GIVEN** a `S2FieldPath fp` pointing at a vector field, and a trim `count`
- **WHEN** `from = fp.childAt(count)` and `to = fp.upperBoundForSubtreeAt(fp.last())`
- **THEN** `from.compareTo(to) <= 0`
- **AND** every descendant `fp.childAt(i)` with `i >= count` sorts in `[from, to)`

### Requirement: S2FieldPathBuilder is separate from S2FieldPath

`S2FieldPathBuilder` SHALL be an interface that is NOT a subtype of `S2FieldPath`. It SHALL provide:

- `void set(int i, int v)`, `void down()`, `void up(int n)`, `void inc(int n)` — in-place mutation of the current path cursor.
- `int get(int i)`, `int last()` — read-only access to the current cursor state.
- `S2FieldPath snapshot()` — returns an immutable `S2FieldPath` reflecting the builder's current state; the returned value is independent of subsequent builder mutations.

Each `S2FieldPath` implementor SHALL have exactly one matching `S2FieldPathBuilder` implementor whose `snapshot()` returns that path type.

#### Scenario: Builder is not a S2FieldPath

- **WHEN** attempting to assign a `S2FieldPathBuilder` to a `S2FieldPath` variable
- **THEN** the compiler rejects the assignment (no subtype relationship)

#### Scenario: Snapshot is independent of subsequent mutations

- **GIVEN** a `S2FieldPathBuilder b` after some sequence of `set/down/up/inc` calls
- **WHEN** `S2FieldPath fp = b.snapshot()` is obtained, then `b` is further mutated
- **THEN** `fp` continues to reflect the state of `b` at the snapshot moment
- **AND** `fp.equals(fp)` and `fp.hashCode()` are stable across subsequent `b` mutations

### Requirement: S2FieldPathType enum pairs path impl with builder factory

`S2FieldPathType` SHALL be a Java `enum` in `skadistats.clarity.model.s2`. Each constant SHALL represent a concrete pairing `(S2FieldPath implementation, S2FieldPathBuilder factory)`. The enum SHALL expose a `newBuilder()` method that returns a fresh `S2FieldPathBuilder` matched to that path impl.

The enum SHALL have at least one constant, `LONG`, which pairs `S2LongFieldPath` with a supplier of `S2LongFieldPathBuilder`. Adding a new path impl SHALL require exactly one enum constant, one `S2FieldPath` implementor, and one `S2FieldPathBuilder` implementor; no other state or reader code needs to change.

#### Scenario: LONG constant exists and matches the current production impl

- **WHEN** `S2FieldPathType.LONG.newBuilder()` is called
- **THEN** an instance of `S2LongFieldPathBuilder` is returned
- **AND** its `snapshot()` returns a `S2LongFieldPath`

#### Scenario: Misconfiguration is statically impossible

- **WHEN** attempting to mix a builder from one `S2FieldPathType` constant with a path consumer expecting another impl
- **THEN** either the compiler rejects the call (if the consumer is typed on the concrete path impl)
- **OR** the operation succeeds polymorphically via the `S2FieldPath` interface — both cases preserve correctness

### Requirement: Runner exposes withFieldPathType configuration

`SimpleRunner` and `ControllableRunner` SHALL expose a `withFieldPathType(S2FieldPathType type)` configuration method analogous to the existing `withStateType(...)`. The chosen `S2FieldPathType` SHALL be propagated to `S2FieldReader` construction so that builders originating inside the reader match the configured impl.

The default, in the absence of an explicit call, SHALL be `S2FieldPathType.LONG` — preserving current behaviour for all existing users.

#### Scenario: Default selects LONG

- **WHEN** a runner is constructed without calling `withFieldPathType(...)`
- **THEN** `S2FieldPathType.LONG` is used
- **AND** observable replay output is bit-exact identical to pre-change behaviour

#### Scenario: Explicit selection propagates to the reader

- **WHEN** `runner.withFieldPathType(S2FieldPathType.LONG)` is called before run
- **AND** parsing begins
- **THEN** `S2FieldReader` obtains builders via `S2FieldPathType.LONG.newBuilder()`
- **AND** all field paths seen by the state and downstream consumers are instances of the corresponding `S2FieldPath` impl
