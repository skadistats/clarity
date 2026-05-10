## Why

`Entities` exposes its query API as `Iterator<Entity>` returned from
`getAllByPredicate` / `getAllByDtName`, with `getByPredicate` /
`getByDtName` convenience methods that silently pick the first match.
This shape predates Java 8 — it was hand-rolled in 2015 to drop the
Guava dependency, on top of an internal `SimpleIterator` helper that
mimics `com.google.common.collect.AbstractIterator`. The single-match
helpers also lie about cardinality (a DT class can have many live
entities; `getByDtName` silently returns one).

`util.SimpleIterator` and `util.Iterators` are leftovers from the same
era. With Java 21 baseline, JDK streams cover the consumer-facing
iteration cleanly, and the two remaining `SimpleIterator` callers
(both internal `fieldPathIterator` implementations on S1 entity
states) are bounded scans that read more naturally as inline `Iterator`
instances. `util.Iterators.emptyIterator` has no callers at all.

## What Changes

- **BREAKING**: Replace `Entities.getAllByPredicate(Predicate<Entity>)`,
  `Entities.getByPredicate(Predicate<Entity>)`,
  `Entities.getAllByDtName(String)` and `Entities.getByDtName(String)`
  with a single `Entities.stream() : Stream<Entity>` plus a static
  `Entities.byDtName(String) : Predicate<Entity>` helper. The old
  single-match shape is removed deliberately — callers must explicitly
  pick `findFirst`, `forEach`, etc., instead of having the API silently
  decide.
- Inline the two S1 `fieldPathIterator` implementations
  (`S1ObjectArrayEntityState`, `S1FlatEntityState`) as plain
  `Iterator<FieldPath>` anonymous classes. No behavioral change; just
  drops the dependency on `SimpleIterator`.
- Delete `skadistats.clarity.util.SimpleIterator` (no remaining
  callers after the two changes above).
- Delete `skadistats.clarity.util.Iterators` (already orphaned —
  zero callers in tree).
- `EntityState.fieldPathIterator()` keeps its `Iterator<FieldPath>`
  return type. The two-pointer merge walk in
  `StateDifferenceEvaluator` requires pull-based iteration; converting
  to `Stream` would force `.iterator()` round-trips on a per-entity
  hot path. S2 implementations are not touched (they already use
  dedicated iterators or list-iterators, no `SimpleIterator`
  involvement).

## Capabilities

### New Capabilities
- `entities-query-api`: Public `Entities` query surface for locating
  entities by predicate or DT class name.

### Modified Capabilities
<!-- None. `fieldPathIterator` behavior is unchanged; the S1 refactor
     is implementation-internal. No existing spec covers the legacy
     `Entities.getAllByPredicate`/`getByDtName` API. -->

## Impact

- **clarity (parser)**: `Entities` public API, two S1 entity-state
  files, two util classes deleted.
- **clarity-examples**: two callers (`examples/cooldowns/Cooldowns.java`,
  `examples/matchend/Main.java`) migrate from `getByDtName` to
  `stream().filter(byDtName(...)).findFirst()`.
- **clarity-analyzer**: no callers of the affected `Entities`
  methods or of `SimpleIterator`/`Iterators`. Unaffected.
- **Downstream user code**: any consumer using
  `getAllByPredicate` / `getByPredicate` / `getAllByDtName` /
  `getByDtName` must migrate to `stream()`. Migration is mechanical
  but the `getByDtName(...)` → `findFirst()` rewrite makes the
  cardinality assumption explicit, which is the point. Known
  downstream callers outside this workspace include
  **odota-parser** (`opendota.Parse` and its bench harness) — they
  migrate independently in their own repo.
