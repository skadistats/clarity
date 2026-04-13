## Why

The Field classes (ValueField, VectorField, PointerField, etc.) have `setValue`/`getValue`/`ensureCapacity` methods that take `NestedEntityState` and directly manipulate state storage. This couples all S2 EntityState implementations to the `NestedEntityState` interface — even when their storage isn't nested. TreeMapEntityState needs an artificial View adapter for this, and the planned FlatEntityState cannot meaningfully implement NestedEntityState.

The actual write chain is: `S2FieldReader` decodes values → `FieldChanges` collects (FieldPath, Object) pairs → `state.setValueForFieldPath(fp, value)`. The FieldReader already knows the Field type at decode time but discards that information. Meanwhile, the State has to rediscover the Field type during traversal to handle structural operations (vector resize, pointer serializer switching) differently from normal value writes.

The fix: introduce `StateMutation` as a sealed interface that explicitly models the different kinds of mutations. The FieldReader asks each Field to create the right StateMutation via `field.createMutation(decodedValue)`. FieldChanges carries `StateMutation[]` instead of `Object[]`. Each State implementation receives typed mutations and handles them according to its own storage model — no NestedEntityState coupling, no hidden-field-path detection, no View adapters.

## What Changes

- **New `StateMutation` sealed interface** with variants `WriteValue(Object)`, `ResizeVector(int count)`, `SwitchPointer(Serializer newSerializer)` — replaces the untyped `Object value` in the write chain
- **New `Field.createMutation(Object decodedValue)` method** — each Field subclass creates the appropriate StateMutation. Replaces `setValue`, `getValue`, `ensureCapacity`, and `isHiddenFieldPath` with a single polymorphic method
- **BREAKING**: `setValue`, `getValue`, `ensureCapacity`, `isHiddenFieldPath` removed from Field classes
- **BREAKING**: `EntityState.setValueForFieldPath(FieldPath, Object)` replaced by `EntityState.applyMutation(FieldPath, StateMutation)`
- **BREAKING**: `FieldChanges` carries `StateMutation[]` instead of `Object[]`
- **BREAKING**: `S2EntityState` dissolved — each State implementation gets its own `applyMutation`/`getValueForFieldPath`
- `S2FieldReader` uses `field.createMutation(decoded)` instead of just storing the raw decoded value
- `NestedEntityState` becomes an internal detail of `NestedArrayEntityState`
- `NestedArrayEntityState` gets its own traversal with Field+Entry navigation, dispatches on StateMutation at the leaf
- `TreeMapEntityState` loses the View adapter, dispatches on StateMutation directly: `WriteValue` → put, `ResizeVector` → trim, `SwitchPointer` → clear and replace
- Structural operations (`ResizeVector`, `SwitchPointer`) don't store values → `fieldPathIterator` naturally skips them without hidden-field-path checks

## Capabilities

### New Capabilities
- `state-mutation`: Sealed `StateMutation` interface modeling typed state mutations, with `Field.createMutation` for polymorphic creation

### Modified Capabilities
- `nested-entity-state`: S2EntityState loses shared traversal, NestedEntityState becomes an internal detail of NestedArrayEntityState, `setValueForFieldPath` replaced by `applyMutation`
- `treemap-nested-state`: TreeMapEntityState no longer extends S2EntityState, no longer implements NestedEntityState, loses the View adapter, dispatches on StateMutation directly

## Impact

- `skadistats.clarity.model.state.StateMutation` — new sealed interface with WriteValue, ResizeVector, SwitchPointer records
- `skadistats.clarity.io.s2.Field` — `setValue`, `getValue`, `ensureCapacity`, `isHiddenFieldPath` removed; new `createMutation(Object)` method (default: `WriteValue`)
- `skadistats.clarity.io.s2.field.VectorField` — overrides `createMutation` to produce `ResizeVector`
- `skadistats.clarity.io.s2.field.PointerField` — overrides `createMutation` to produce `SwitchPointer`
- `skadistats.clarity.io.s2.S2FieldReader` — uses `field.createMutation(decoded)` to produce StateMutations
- `skadistats.clarity.io.s1.S1FieldReader` — wraps decoded values in `WriteValue`
- `skadistats.clarity.io.FieldChanges` — `Object[] values` replaced by `StateMutation[] mutations`
- `skadistats.clarity.model.state.EntityState` — `setValueForFieldPath(FieldPath, Object)` replaced by `applyMutation(FieldPath, StateMutation)`
- `skadistats.clarity.model.state.S2EntityState` — dissolved
- `skadistats.clarity.model.state.NestedArrayEntityState` — own traversal, dispatches on StateMutation
- `skadistats.clarity.model.state.TreeMapEntityState` — own traversal (direct TreeMap ops), dispatches on StateMutation, View class removed
- `skadistats.clarity.model.state.NestedEntityState` — internal detail of NestedArrayEntityState
- `skadistats.clarity.model.state.ObjectArrayEntityState` — updated to implement `applyMutation` (S1, only WriteValue)
- Prepares for `flat-entity-state` change: FlatEntityState dispatches on StateMutation with its own FieldLayout-based traversal
