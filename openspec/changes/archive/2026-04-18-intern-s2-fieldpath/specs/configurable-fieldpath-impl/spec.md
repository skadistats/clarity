## ADDED Requirements

### Requirement: S2LongFieldPath instances are interned JVM-wide

`S2LongFieldPath` SHALL maintain a single process-wide intern table keyed by the wrapped `long id`. Every `S2LongFieldPath` instance SHALL be obtained through a static factory `S2LongFieldPath.of(long id)` that returns the existing wrapper for the given `id` if one exists, or atomically creates and registers a new one if not. The class's constructor SHALL be private (or otherwise inaccessible to external code) so that the factory is the sole construction path.

The intern table SHALL be safe for concurrent reads from multiple parser threads with no synchronization in the read fast path, and SHALL guarantee that no two `S2LongFieldPath` instances with the same `id` are ever observable simultaneously, regardless of concurrent calls to `of(long)`.

`S2LongFieldPathBuilder.snapshot()` SHALL produce its result via `S2LongFieldPath.of(id)`. Direct construction of `S2LongFieldPath` outside the factory is not permitted.

#### Scenario: of(long) returns the same instance for the same id

- **GIVEN** any two calls `S2LongFieldPath.of(id1)` and `S2LongFieldPath.of(id2)` with `id1 == id2`
- **WHEN** both calls have completed
- **THEN** both return the same object reference (`a == b`)

#### Scenario: snapshot routes through the intern

- **GIVEN** a `S2LongFieldPathBuilder b1` and `S2LongFieldPathBuilder b2` mutated to represent the same logical path
- **WHEN** `var p1 = b1.snapshot()` and `var p2 = b2.snapshot()`
- **THEN** `p1 == p2`

#### Scenario: Concurrent first-time interning produces a single instance

- **GIVEN** N parser threads concurrently call `S2LongFieldPath.of(id)` for an `id` not yet present in the intern
- **WHEN** all calls have returned
- **THEN** exactly one `S2LongFieldPath` instance exists for `id` in the intern
- **AND** every caller observes the same reference

### Requirement: S2LongFieldPath equals is identity-based

Because every distinct `id` corresponds to exactly one `S2LongFieldPath` instance JVM-wide, `S2LongFieldPath.equals(Object)` SHALL be implemented as reference identity (`this == o`). The behaviour SHALL remain consistent with `hashCode()` and with the `Comparable<S2FieldPath>` contract from the existing requirements.

`S2LongFieldPath.hashCode()` SHALL continue to be derived from the wrapped `long id` (not from `System.identityHashCode`) so that hash-based collections retain good bucket distribution.

`S2LongFieldPath.compareTo(S2FieldPath)` SHALL continue to use the long-encoded lexicographic ordering and SHALL remain consistent with `equals` (i.e. `a.compareTo(b) == 0` iff `a == b`).

#### Scenario: Reference identity matches value equality

- **GIVEN** any two `S2LongFieldPath` references `a` and `b` obtained through any combination of `of(long)` or `snapshot()` calls
- **WHEN** `a.equals(b)` is evaluated
- **THEN** the result equals `(a == b)`
- **AND** `a.equals(b)` implies `a.hashCode() == b.hashCode()`
- **AND** `a.equals(b)` implies `a.compareTo(b) == 0`

#### Scenario: hashCode is derived from id, not identity

- **GIVEN** a `S2LongFieldPath` instance `p` with id `i`
- **WHEN** `p.hashCode()` is evaluated
- **THEN** the result equals `S2LongFieldPathFormat.hashCode(i)`
- **AND** the result is independent of the JVM's identity hash for `p`
