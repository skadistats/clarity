## Purpose

Defines a stream-based query API on the `Entities` processor for
locating live entities, replacing ad-hoc index loops and hidden
single-result helpers. Callers compose standard JDK terminal
operations and express cardinality (first / any / all) explicitly.

## Requirements

### Requirement: Stream of live entities

The `Entities` processor SHALL expose a `stream() : Stream<Entity>`
method returning every currently-live entity in ascending entity-index
order. Slots that are empty (no entity at that index) MUST be skipped.

#### Scenario: Iterating yields all live entities in index order

- **WHEN** a caller invokes `entities.stream().toList()`
- **THEN** the returned list contains exactly the entities for which
  `entities.getByIndex(i)` is non-null, ordered by ascending `i`

#### Scenario: Empty index range produces empty stream

- **WHEN** no entities are live (e.g. before any `CREATE` has been
  processed)
- **THEN** `entities.stream().count()` returns 0

#### Scenario: Stream composes with standard JDK terminal operations

- **WHEN** a caller writes
  `entities.stream().filter(p).findFirst()` for an arbitrary
  predicate `p`
- **THEN** the result is the first live entity in index order
  satisfying `p`, or `Optional.empty()` if none match

### Requirement: DT-class-name predicate factory

The `Entities` processor SHALL expose a static
`byDtName(String) : Predicate<Entity>` factory that returns a
predicate matching entities whose `DTClass.getDtName()` equals the
given name (case-sensitive, exact match).

#### Scenario: Predicate matches entities of the named DT class

- **WHEN** a caller filters
  `entities.stream().filter(Entities.byDtName("CDOTAGamerulesProxy"))`
- **THEN** the resulting stream contains exactly the entities whose
  DT class name equals `"CDOTAGamerulesProxy"`

#### Scenario: Predicate returns false for non-matching name

- **WHEN** the predicate is applied to an entity whose DT class name
  differs from the supplied name
- **THEN** `Predicate#test` returns `false`

### Requirement: Cardinality is the caller's responsibility

The query API SHALL NOT provide single-result convenience methods
that hide multiplicity. Callers expecting a single match MUST
express that intent explicitly via stream terminal operations
(`findFirst`, `findAny`).

#### Scenario: Caller picks first match explicitly

- **WHEN** a caller wants any single entity of a given DT class
- **THEN** they write
  `entities.stream().filter(Entities.byDtName(name)).findFirst()`
