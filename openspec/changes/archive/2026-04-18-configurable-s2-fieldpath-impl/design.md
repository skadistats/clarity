## Context

`S2FieldPath` is currently sealed over two branches — `S2LongFieldPath` (immutable, used as map keys and event payload) and `S2ModifiableFieldPath` (mutable scratch cursor used by `FieldOp.execute`). The branches share only the `get(i)/last()` read surface. Nothing in the codebase consumes them polymorphically: readers always write to the modifiable, states always key on the immutable.

That false sibling relationship is the proximate cause of the `(S2LongFieldPath) fpX` downcasts at the top of every `S2TreeMapEntityState.write` / `applyMutation` method. It is also why `S2TreeMapEntityState` reaches through `fp.id()` into `S2LongFieldPathFormat.set/down/last` — because the `S2FieldPath` interface is too weak to express range queries, the state bypasses it.

Zooming out: we have exactly one field-path representation in production (long-encoded) and no structural way to swap it. `clarity` has collected three alternate entity-state representations over the years (FLAT, NESTED_ARRAY, TREE_MAP), each gated by `S2EntityStateType`. The field-path dimension has no equivalent knob. The goal is to introduce one and, in the process, make the type hierarchy tell the truth about what is immutable vs. mutable.

Motivation is craft-driven ("Liebhaberei") rather than a concrete second impl waiting in the wings. A benchmark regression is the accept/revert gate.

## Goals / Non-Goals

**Goals:**
- `S2FieldPath` is the immutable key contract. Every implementor is safe as a map key, comparable, and supports range construction.
- Builders are a distinct concept, paired 1:1 with their concrete path impl through a `S2FieldPathType` enum.
- `S2TreeMapEntityState.write` / `applyMutation` take `S2FieldPath` directly with no casts and no reference to `S2LongFieldPathFormat`.
- The field-path impl is selected on the runner via `withFieldPathType(...)`, mirroring `withStateType(...)`.
- Default behaviour preserved: without explicit configuration, `LONG` is selected and performance matches the current release.

**Non-Goals:**
- Shipping a second field-path impl. Only `LONG` needs to exist after this change; the scaffolding just makes adding one cheap.
- Generic `<P extends S2FieldPath>` threading through state / reader APIs. That orthogonality was considered and rejected (see Decision 3).
- Changing the cross-engine `FieldPath` contract or any S1 path code.
- Changing the event-listener surface for `@OnEntityPropertyChanged` and friends.

## Decisions

### Decision 1: Drop the modifiable branch from sealed `S2FieldPath`

**Choice:** `S2FieldPath` is sealed over `S2LongFieldPath` (and any future immutable impls) only. `S2ModifiableFieldPath` is removed from the hierarchy. Its replacement, `S2FieldPathBuilder`, is an unrelated interface living alongside `S2FieldPath`.

**Alternatives:**
- Insert an `S2ImmutableFieldPath` middle tier. Works but adds a layer whose only job is "the subset of `S2FieldPath` that is safe as a map key." Simpler to make the top-level interface itself that contract.
- Keep the current hierarchy and cast at boundaries. Today's state; the whole point of the change is to remove it.

**Rationale:** The modifiable path is never consumed polymorphically with the immutable. Removing it from the hierarchy makes the sealing mean something non-trivial: every `S2FieldPath` is immutable, comparable, and range-op-capable.

### Decision 2: Range operations live on `S2FieldPath`

**Choice:** `S2FieldPath` gains `childAt(int index)` and `upperBoundForSubtreeAt(int depth)`. Implementations return `S2FieldPath` (the interface), letting callers stay impl-agnostic. (`parent()` and `withIndexAt(depth, index)` were scoped out during implementation — neither was needed once `childAt` + `upperBoundForSubtreeAt` covered both the tree-map range-op call sites.)

**Alternatives:**
- Keep range-op logic inside state impls, branching on concrete path type. Today's arrangement; the source of the `S2LongFieldPathFormat` leak.
- F-bounded `S2FieldPath<SELF>` so range ops return `SELF`. Unnecessary here — state code never needs the concrete return type.

**Rationale:** The tree map's subrange-as-descendants technique only works because path ordering matches a specific lexicographic relationship. Encoding that relationship in interface methods means the tree map can compose bounds without knowing the encoding.

### Decision 3: Non-generic state interface

**Choice:** `S2EntityState` stays non-generic. `S2TreeMapEntityState.write` signature is `boolean write(S2FieldPath fp, Object v)`. The internal `TreeMap` is keyed on `S2FieldPath`.

**Alternatives considered:**
- `S2EntityState<P extends S2FieldPath>` with concrete subclasses binding `P`. Adds a type parameter that has to thread through `S2FieldReader<P>`, `FieldChanges<P>`, and downstream consumers.
- `S2EntityState<P>` plus F-bounded `S2FieldPath<SELF extends S2FieldPath<SELF>>` to get `P`-typed returns from range ops. Deepens the rabbit hole further.

**Rationale:** Java generics erase at runtime. The hot-path virtual dispatch we pay (see Risk below) is identical in the generic and non-generic versions. Generics would buy compile-time sharpness at the cost of pervasive type-parameter noise, with no perf benefit. The user explicitly preferred avoiding viral generics.

### Decision 4: `S2FieldPathType` enum as the configuration knob

**Choice:** A Java enum whose constants pair a concrete `S2FieldPath` impl with a `Supplier<S2FieldPathBuilder>`. The runner holds a `S2FieldPathType` chosen via `withFieldPathType(...)`. `S2FieldReader` asks its configured type for a builder.

**Alternatives:**
- Inject a `PathFactory` object directly. Equivalent, more verbose; the enum form names the available impls and forbids accidental mixes.
- Service loader / SPI discovery. Overkill for what is a closed set of impls living in clarity itself.
- Static `S2FieldPath.newBuilder()` + reflection per impl. Loses the pairing discipline.

**Rationale:** Matches the existing `S2EntityStateType` pattern; adding a new impl means adding one enum constant plus two classes; misconfiguration is statically impossible.

### Decision 5: `FieldOp.execute` takes `S2FieldPathBuilder`

**Choice:** Signature moves from `FieldOp.execute(int, S2ModifiableFieldPath, BitStream)` to `FieldOp.execute(int, S2FieldPathBuilder, BitStream)`. Today's single impl becomes `S2LongFieldPathBuilder`; the method body unchanged.

**Alternatives:**
- Keep `S2ModifiableFieldPath` as a class name. Rejected — the type is not a path, it is a builder; naming it correctly is part of the cleanup.
- Generic `FieldOp.<B extends S2FieldPathBuilder>execute(...)`. Needless; the builder is virtual-dispatched via interface methods either way.

**Rationale:** The signature tells the truth about the collaboration: FieldOp consumes a mutable builder and never sees an immutable path.

### Decision 6: Benchmark gate, with revert as the default

**Choice:** Merge only if the JMH harness (`./gradlew bench`) and the full-replay smoke benchmark on the Dota 2 corpus both show ≤2% regression versus the baseline commit. If the regression is higher, open a follow-up to investigate before deciding to merge anyway.

**Rationale:** The hot-path virtualization (compare, range-op, FieldOp-on-builder) has the same shape as every other monomorphic-to-interface refactor — HotSpot CHA usually inlines fine, but sometimes the profile doesn't stabilize and you eat a percent. The change is Liebhaberei; it should not cost real users throughput.

## Risks / Trade-offs

- **[Hot-path virtual dispatch]** `S2LongFieldPath.compareTo` today is a monomorphic `Long.compare`. After the change it is an interface dispatch. HotSpot CHA will monomorphize when only one impl is loaded, but guard-inlining has a small fixed cost. → Mitigation: benchmark gate (Decision 6). Secondary mitigation: `S2LongFieldPath.compareTo` can `instanceof`-fast-path when the argument is the same concrete type — cheap and keeps the single-impl case sharp.
- **[Allocation pressure from range-op methods]** `fp.childAt(...)` / `fp.upperBoundForSubtreeAt(...)` return a new `S2FieldPath` each call. Today's `trimEntries` constructs two `S2LongFieldPath` wrappers; the new shape constructs the same two but through a virtual call. Escape analysis should still scalar-replace the single-field `S2LongFieldPath`. → Mitigation: benchmark gate; inspect JIT logs on hot methods if a regression appears.
- **[Sealed-hierarchy breakage for external consumers]** `S2FieldPath` has `sealed permits S2LongFieldPath, S2ModifiableFieldPath` today. Downstream code that pattern-matches on `S2ModifiableFieldPath` via `S2FieldPath` will break. → Mitigation: grep `clarity-analyzer` and any other known consumer; there is no legitimate reason to pattern-match the modifiable branch, and we expect none.
- **[Spec `fp.s2()` drift]** The existing `treemap-nested-state` spec mentions `fp.s2()` which no longer exists in code. Unrelated to this change but close to the blast radius. → Mitigation: update that spec text as part of this change to match the real signatures.
- **[Configuration complexity]** Adding a second runner knob means users can now misconfigure in two dimensions. Practically only one path impl will exist for the foreseeable future, so the knob is latent flexibility. → Mitigation: default stays `LONG`; documented as "leave alone unless you know what you are doing."

## Migration Plan

Staged within one PR, ordered so each stage compiles and tests pass:

1. **Reshape the hierarchy.** Remove `S2ModifiableFieldPath`/`S2LongModifiableFieldPath` from the `S2FieldPath` seal; rename/relocate them to `S2FieldPathBuilder` / `S2LongFieldPathBuilder`. Add range-op methods to `S2FieldPath`. Tests still pass; `S2TreeMapEntityState` still casts.
2. **Introduce `S2FieldPathType`.** Enum with a single `LONG` constant today. `S2FieldReader` obtains builder from type; default path unchanged.
3. **Rewrite `S2TreeMapEntityState` against `S2FieldPath`.** Remove casts; replace `S2LongFieldPathFormat` calls with interface method calls. Same for `S2FlatEntityState` and `S2NestedArrayEntityState` boundary code.
4. **Expose the runner knob.** `withFieldPathType(...)` on `SimpleRunner` and `ControllableRunner`; default `LONG`.
5. **Benchmark.** Run `./gradlew bench` plus full-replay comparison on the Dota 2 corpus. Compare against the pre-change baseline commit.
6. **Decide.** If within the ≤2% gate, merge. If not, investigate or revert.

**Rollback strategy:** The change is a single PR. Revert is straightforward — no data format changes, no replay-file compatibility concerns, no external API shifts.

## Open Questions

- `S2FieldPathBuilder` naming: is `S2FieldPathBuilder` best, or would `S2FieldPathCursor` communicate the mutable-cursor lifecycle more clearly? Decide during implementation; either works.
- ~~Should `S2FieldPath.parent()` exist or should the state always compose from the current `fp` via `withIndexAt`?~~ Resolved: neither method is needed. The two call sites (`trimEntries`, `clearSubEntries`) composed cleanly from `childAt` + `upperBoundForSubtreeAt` alone. Interface stayed minimal.
- The existing spec file `treemap-nested-state/spec.md` references `fp.s2()` — unclear if that came from a planned-but-reverted change or is stale. Confirm during Stage 3 spec update.
