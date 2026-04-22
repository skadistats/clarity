## Why

Post `inline-field-mutation-apply` and `flat-entity-state`, the parser writes decoded primitive values directly into flat state slots without ever materializing a wrapper object on the decode path. The accessor contract, however, still goes through `Object` — every consumer read (`entity.getProperty(fp)`, `state.getValueForFieldPath(fp)`) re-boxes an `int` / `long` / `float` on the way out.

This re-boxing is the only remaining cost preventing consumers from staying primitive end-to-end. It also blocks a structural win for downstream UIs like clarity-analyzer: they currently `state.copy()` the entire entity on every `@OnEntityUpdated` so an FX-thread action can read arbitrary fields later. A sparse per-field primitive snapshot would replace the full copy — but that requires primitive accessors on a snapshot object, not just on live state.

The companion investigation into markus-wa/demoinfocs-golang's 2026 AI-assisted perf pass (`mw/perf2026`, ~19% throughput) identified two allocation-cache ideas (pre-boxed `simulationTime` cache, per-reader f32 direct-mapped cache). Both are band-aids for a boxing decoder; clarity's primitive flat state makes the disease preventable rather than curable. Primitive accessors are the dual-use API that makes that already-done work visible to consumers.

## What Changes

- Add primitive getters on `State` / `Entity`: `getInt(FieldPath)`, `getLong(FieldPath)`, `getFloat(FieldPath)`, plus `getObject(FieldPath)` as the escape hatch for non-scalar field types (Vector, String, handles already surfaced as structured types). Getters read directly off the flat slot backing; no boxing on the return path.
- Add `State.captureChanged(FieldPath[] fps, int num) → StateDelta`: a sparse snapshot sized to `num` that holds only the named field paths' primitive values. Intended for cross-thread hand-off where the full state would otherwise need to be copied.
- Add `StateDelta` type with the same primitive-getter contract as `State`, plus `FieldPath[] fields()` to enumerate what it covers. Queries for unknown fields return a documented default (zero for primitives, `null` for `getObject`) — no exception.
- Add `State.applyFrom(StateDelta delta, FieldPath fp)` as the in-place merge primitive: consumers holding a long-lived FX-side `State` can absorb a delta into a single slot without allocating.
- No breaking changes. The existing `getValueForFieldPath` / `getProperty` paths remain, layered over the new primitive accessors (box on the way out only when asked via the generic path).

## Capabilities

### New Capabilities
- `primitive-state-accessors`: primitive-typed read contract on `State` / `Entity` plus a sparse `StateDelta` snapshot for cross-thread or deferred-apply consumers, with an in-place `applyFrom` merge primitive for long-lived consumer-side states.

## Impact

- `src/main/java/skadistats/clarity/model/state/` — new `StateDelta` interface, implementation class sized to `num`.
- `src/main/java/skadistats/clarity/model/state/EntityState.java` (or wherever the concrete flat state lives post `flat-entity-state`) — primitive accessor methods, `captureChanged`, `applyFrom`.
- `src/main/java/skadistats/clarity/model/Entity.java` — thin delegating primitive getters.
- No impact to decoder, dispatch, or processor wiring — this is an additive read-side API.
- Downstream consumer `clarity-analyzer` has a companion change (`sparse-state-delta-updates`) that depends on this landing first.

## Non-goals (explicit)

- **Primitive-typed `@OnEntityPropertyChanged` handlers.** The dispatch signature still boxes when invoking generic listener methods. A follow-up change could introduce primitive-specialized handler shapes (`@OnEntityIntPropertyChanged`, `FloatConsumer`-shaped callback), but that is API-surface growth and narrower than the pull-side win. Scoped out; revisit once consumer demand is visible.
- **Allocation caches inside the decoder** (pre-boxed sim-time, per-reader f32 cache from demoinfocs-golang `mw/perf2026`). Clarity's primitive decode path already never boxes, so these are moot on the parse side. They would only apply at the listener-dispatch boundary if we kept the boxed-push API — which is exactly what the non-goal above leaves open.
