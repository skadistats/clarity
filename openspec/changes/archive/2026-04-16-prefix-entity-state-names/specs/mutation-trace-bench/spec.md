## MODIFIED Requirements

### Requirement: Births are reproduced at @Setup(Invocation) as setup work

For each `BirthRecipe` in capture order, the replay harness SHALL (a) construct an `EntityState`
— either fresh via `S2EntityStateType.createState(SerializerField, ...)` for an `EMPTY` birth, or
via `states[srcStateId].copy()` for a `COPY_OF` birth — and (b) apply the birth's captured
`setupMutations` (the mutations that landed during baseline build / entity create / entity
recreate). All of this SHALL happen in `@Setup(Level.Invocation)`, outside the measured window.

The hot loop SHALL apply ONLY update mutations (those originating from the entity-update code
path), against states that are already in their post-create shape.

#### Scenario: EMPTY birth produces the requested impl
- **WHEN** a BirthRecipe of kind `EMPTY` is materialized with `impl=FLAT`
- **THEN** the resulting `EntityState` is a `S2FlatEntityState` built from the recipe's `SerializerField`

#### Scenario: COPY_OF birth clones its source
- **WHEN** a BirthRecipe of kind `COPY_OF` with `srcStateId=S` is materialized
- **THEN** the resulting `EntityState` is `states[S].copy()` — same impl, independent contents

#### Scenario: Setup mutations are pre-applied, not measured
- **WHEN** the measured loop starts
- **THEN** every state in the `states[]` array holds the logical contents it would have had in production immediately after its `setState(...)` installation
