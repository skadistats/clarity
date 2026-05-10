## Context

`Entities` and the `util.SimpleIterator` / `util.Iterators` helpers
were introduced in commit `6d4a281` (April 2015) to remove the Guava
dependency on a Java 7 baseline — Guava's `AbstractIterator`,
`Iterators.emptyIterator()` and `Predicate` each got a hand-rolled
JDK-only equivalent. The `Predicate` half was already replaced with
`java.util.function.Predicate` years ago. `SimpleIterator` and
`Iterators` are the leftovers; the `Entities` query API still wears
the iterator-and-Guava-style shape it inherited from that era.

Current shape:
- `Entities.getAllByPredicate(Predicate<Entity>) : Iterator<Entity>`
- `Entities.getByPredicate(Predicate<Entity>) : Entity`
- `Entities.getAllByDtName(String) : Iterator<Entity>`
- `Entities.getByDtName(String) : Entity`

Three internal callers of `SimpleIterator` after migration:
- `Entities.getAllByPredicate` — public API, replaced
- `S1ObjectArrayEntityState.fieldPathIterator` — internal
- `S1FlatEntityState.fieldPathIterator` — internal

`util.Iterators` has zero callers anywhere in the tree (parser,
tests, jmh, examples, analyzer).

`EntityState.fieldPathIterator()` itself is not changing shape —
it stays `Iterator<FieldPath>` because `StateDifferenceEvaluator`
walks two of them in lockstep with merge-sort logic, which is a
pull pattern that streams cannot express without round-tripping
back to an iterator.

## Goals / Non-Goals

**Goals:**
- Public `Entities` query API expressed in JDK-native streams.
- Remove `util.SimpleIterator` and `util.Iterators` outright.
- Make cardinality explicit at call sites — no silent
  "first-of-many" helpers.
- Net code reduction in `Entities` and the two S1 entity-state
  files. Zero behavioral change in `fieldPathIterator`.

**Non-Goals:**
- Touching S2 `fieldPathIterator` implementations
  (`S2TreeMapEntityState`, `S2NestedArrayEntityState`,
  `S2FlatEntityState`). They use real iterators / dedicated classes
  already; nothing to clean up.
- Changing `EntityState.fieldPathIterator()`'s return type.
- Adding a `forEachFieldPath(Consumer<FieldPath>)` push API. That
  would be a real win for `S2FlatEntityState` (which currently
  eagerly materializes into a `List` before returning
  `list.iterator()`) and for single-iterator consumers like
  `PropertyChange` — but it would have to live alongside
  `fieldPathIterator`, since the merge-walk needs pull. Out of
  scope; track separately.
- Touching `model.Vector`. Discussed alongside this cleanup but
  has its own design questions (record vs. value-class vs.
  `float[]`); separate change.

## Decisions

### Decision: Replace four query methods with `stream() + byDtName(...)`

Drop `getAllByPredicate`, `getByPredicate`, `getAllByDtName` and
`getByDtName`. Add:

```java
public Stream<Entity> stream();
public static Predicate<Entity> byDtName(String dtName);
```

Implementation:

```java
public Stream<Entity> stream() {
    return IntStream.range(0, entityCount)
            .mapToObj(this::getByIndex)
            .filter(Objects::nonNull);
}

public static Predicate<Entity> byDtName(String dtName) {
    return e -> dtName.equals(e.getDtClass().getDtName());
}
```

**Rationale**: One method covers all four old shapes. Cardinality
becomes a caller decision (`findFirst`, `forEach`, `count`, etc.).
Index order is preserved via `IntStream.range`. `byDtName` is the
one filter idiom common enough across examples to justify a static
factory; other patterns are short enough to write inline.

**Alternatives considered:**
- Keep `findByDtName(String) : Optional<Entity>` as a convenience.
  Rejected — it carries the same "silently picks one of many" bug
  shape as the current `getByDtName`. Worse, returning `Optional`
  *suggests* the result is unique, when in practice there can be
  many `CDOTAPlayer` entities. Forcing the caller to write
  `findFirst()` makes the assumption explicit.
- A wider filter family (`byClassId`, `byHandle`, …). Rejected for
  now — `getByHandle`/`getByIndex` already exist as direct lookups
  (not filters), and other filter patterns aren't repeated enough
  in real code to earn their own factory. Easy to add later.

### Decision: Keep `EntityState.fieldPathIterator()` returning `Iterator<FieldPath>`

`StateDifferenceEvaluator.work()` walks two field-path iterators in
lockstep, advancing whichever side is "lower" by `FieldPathUtil`
order. This is a pull-based merge pattern; it cannot be expressed
on `Stream` without `.iterator()` round-trips. Streams also impose
a per-call `Spliterator`+pipeline allocation cost on what is a
per-entity-update hot path. Iterator stays.

### Decision: Inline the two S1 `fieldPathIterator` implementations

Both S1 implementations iterate `0..n` yielding `new S1FieldPath(i)`.
They use `SimpleIterator` only because Guava's `AbstractIterator`
was the original template — they don't actually need the
read-ahead-or-null trick (the bound is known upfront).

Rewrite as plain anonymous `Iterator<FieldPath>`:

```java
return new Iterator<>() {
    int i = 0;
    @Override public boolean hasNext() { return i < n; }
    @Override public FieldPath next() {
        if (i >= n) throw new NoSuchElementException();
        return new S1FieldPath(i++);
    }
};
```

The `NoSuchElementException` guard preserves the documented `Iterator`
contract; `SimpleIterator` got it for free, the inline version needs
to spell it out.

### Decision: Delete `util.Iterators` outright

Zero callers in tree. Not even a deprecation period — there's
nothing to deprecate against. Simple deletion.

### Decision: Static `byDtName` lives on `Entities` itself

Co-locate the filter factory with the surface it filters. Callers
do `import static skadistats.clarity.processor.entities.Entities.byDtName;`
which reads cleanly at use sites:
`entities.stream().filter(byDtName("CDOTAPlayer"))`.

## Risks / Trade-offs

- **[Breaking API change for downstream users]** → Any code calling
  the four removed methods must migrate. Mitigation: migration is
  mechanical and the table in the proposal documents every
  rewrite. The cardinality-explicit shape is a deliberate quality
  improvement, not a side-effect.
- **[Stream allocation cost on hot paths]** → `Entities.stream()` is
  a query-side API, not a per-tick path; allocation overhead is
  irrelevant. The hot-path `fieldPathIterator()` is explicitly *not*
  converted. No measurable perf risk.
- **[Loss of `SimpleIterator` as a future utility]** → Future code
  that wants the read-until-null pattern would have to write the
  6-line iterator inline or pull in a dependency. Acceptable: it
  has had three callers in 10 years, two of which don't actually
  benefit from the pattern.
- **[`Stream.iterator()` round-trip if a downstream user wants
  `Iterator<Entity>`]** → They can call `entities.stream().iterator()`.
  One extra allocation, identical iteration semantics. Negligible.

## Migration Plan

1. Land the parser change (new API + old API removed + S1 inline +
   util deletes).
2. Update the two `clarity-examples` callers in the same change
   stream so the examples build stays green:
   - `examples/cooldowns/Cooldowns.java`
   - `examples/matchend/Main.java`
3. Verify `clarity-analyzer` compiles unchanged (it has no callers
   of the affected surface — confirmed by grep).
4. Release notes call out the four removed methods and the
   one-line migration for each.

No runtime data migration. No staged rollout. The change is
source-incompatible but trivially fixed at the call site.
