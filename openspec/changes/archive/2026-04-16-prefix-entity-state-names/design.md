## Context

`skadistats.clarity.model.state` holds concrete entity-state implementations for both Source 1 and Source 2 replays alongside shared abstractions. Historical naming grew inconsistently: some state classes carry `S1`/`S2` prefixes, others do not. The unprefixed S2 classes (`FlatEntityState`, `NestedArrayEntityState`, `TreeMapEntityState`) appear visually as "the default," which was never the intent. One S1 class (`ObjectArrayEntityState`) is unprefixed too. `AbstractS2EntityState` puts the engine tag in the middle of the name.

This change aligns every state-implementation and state-adjacent interface to a single convention: `S1<Name>` / `S2<Name>`, engine prefix leading.

## Goals / Non-Goals

**Goals:**

- Every entity-state class name in `model/state/` carries the engine prefix (`S1…` or `S2…`) as its leading segment.
- Every state-paired interface (currently `NestedEntityState`) follows the same rule.
- Shared / engine-agnostic types remain unprefixed (`EntityState`, `EntityStateFactory`, `EntityRegistry`, `BaselineRegistry`, `ClientFrame`, `StateMutation`).
- Specs that reference renamed classes are updated in lock-step so archiving propagates the new vocabulary into main specs.

**Non-Goals:**

- No behavior change. All rewrites are pure renames.
- No reorganization into `model/state/s1/` and `model/state/s2/` sub-packages. Prefixing preserves a flat directory where S1/S2 pairs sort together and imports stay unambiguous at the use site.
- No renaming of `FieldLayout`, `FieldLayoutBuilder`, `PrimitiveType`. They are S2-specific layout support, not state classes. Whether they belong under `model/state/` at all is a separate concern best addressed on its own.
- No changes to `S1EntityStateType` or `S2EntityStateType` — already correctly prefixed.

## Decisions

### D1: Prefix position is leading, not embedded

`AbstractS2EntityState` becomes `S2AbstractEntityState`.

Rationale: The convention being established is "engine tag first, then descriptive name." Keeping `Abstract` as the leading word would create a mixed convention where some classes read engine-first and others read role-first. One rule is easier to apply and easier to audit.

*Alternative rejected:* leave `AbstractS2EntityState` as-is. Rejected because it leaves a visible exception that future contributors will either propagate (wrongly) or stumble over.

### D2: Interface `NestedEntityState` also gets the prefix

Rename to `S2NestedEntityState`.

Rationale: The interface is exclusively used as the "sub-state" contract for `S2NestedArrayEntityState` (and `S2TreeMapEntityState` as a nested-position state). It is S2-only in practice. Treating it as "state-adjacent" rather than "state" would be a fine line that muddies the rule.

*Alternative rejected:* leave as engine-neutral. Rejected because no S1 code implements this interface and none is planned to.

### D3: Layout support classes excluded

`FieldLayout`, `FieldLayoutBuilder`, `PrimitiveType` keep their current names.

Rationale: These are data structures and a builder used by the flat-layout decoders — not entity-state implementations. The user's scope is "state names." Renaming these would expand the change without a clear line-drawing principle (e.g., should `EntityRegistry` also get a prefix if it has engine-specific behavior internally?). Stop at the state classes.

*Alternative considered:* fold layout-support renames into this change. Rejected to keep the refactor's scope tight and the line-drawing principle defensible.

### D4: Prefix over sub-package

Keep classes in `model/state/` with prefixed names instead of moving to `model/state/s1/` and `model/state/s2/`.

Rationale: Java package is a namespace; identical short names in two packages can confuse IDE completions and import lists. A flat directory with `S1Foo` / `S2Foo` pairs that sort together is easier to read in the file listing and leaves the import form unambiguous at the use site. The number of files is small (7 renames) — a sub-package split would be disproportionate.

*Alternative rejected:* split into `state/s1/` and `state/s2/` sub-packages, drop prefixes inside them. Rejected per reasoning above.

## Risks / Trade-offs

- **Risk:** External callers that reference renamed classes by name (`import skadistats.clarity.model.state.FlatEntityState`) will fail to compile. → **Mitigation:** verify `clarity-examples`, `clarity-analyzer`, `clarity-cp0-bench` still compile. Callers generally use the `EntityState` interface or the `S{1,2}EntityStateType` enum, both of which are unchanged.
- **Risk:** Spec deltas drift from code if any class-name reference is missed. → **Mitigation:** grep the specs for each old name after renaming and confirm zero residual matches before archiving. The delta scope is pure text substitution, so the check is mechanical.
- **Risk:** Stale comments / javadoc references left behind. → **Mitigation:** include a grep sweep across `src/` in tasks.

## Migration Plan

Single commit, atomic rename. No intermediate state where half the codebase uses old names and half uses new. IDE-driven rename refactor for each class, one at a time, each followed by a compile check; then spec deltas; then downstream compile verification.

No rollback needed beyond `git revert` — the change is purely lexical.
