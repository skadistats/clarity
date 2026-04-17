## MODIFIED Requirements

### Requirement: EntityState.applyMutation replaces setValueForFieldPath

The sealed engine-specific sub-interfaces `S1EntityState` and `S2EntityState` SHALL each provide an `applyMutation` method accepting the engine-typed `FieldPath` subtype and a `StateMutation`: `S1EntityState.applyMutation(S1FieldPath, StateMutation)` and `S2EntityState.applyMutation(S2FieldPath, StateMutation)`. The base `EntityState` interface SHALL NOT declare any `applyMutation` method; callers that need to apply a mutation SHALL narrow their state reference to the appropriate sub-interface first (typically via `switch` or `instanceof` on the sealed hierarchy, or by relying on the engine context in which the call occurs).

Each `applyMutation` method SHALL return `true` if the mutation caused a capacity change, matching the previous return contract. `getValueForFieldPath(FieldPath)` SHALL remain unchanged and SHALL remain on the base `EntityState` interface.

The `setValueForFieldPath(FieldPath, Object)` method SHALL NOT exist on any of the interfaces or implementations.

#### Scenario: applyMutation on S1EntityState

- **WHEN** `applyMutation(S1FieldPath, StateMutation)` is called on any `S1EntityState` implementation
- **THEN** the state applies the mutation according to its own storage model
- **AND** returns true if the mutation caused a structural capacity change
- **AND** the call site passes an `S1FieldPath` directly without requiring a runtime cast inside the method

#### Scenario: applyMutation on S2EntityState

- **WHEN** `applyMutation(S2FieldPath, StateMutation)` is called on any `S2EntityState` implementation
- **THEN** the state applies the mutation according to its own storage model
- **AND** returns true if the mutation caused a structural capacity change
- **AND** the call site passes an `S2FieldPath` directly without requiring a runtime cast inside the method

#### Scenario: Base EntityState has no applyMutation

- **WHEN** the `EntityState` base interface is inspected
- **THEN** it does NOT declare any method named `applyMutation`
- **AND** any caller that holds a bare `EntityState` reference must narrow it via `switch` / `instanceof` over the sealed sub-interfaces before invoking `applyMutation`
