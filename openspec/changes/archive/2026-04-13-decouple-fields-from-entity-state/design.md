## Context

S2 Field types interact with entity state through the `ArrayEntityState` interface. Currently only `NestedArrayEntityState` implements this interface, and its `setValueForFieldPath`/`getValueForFieldPath` methods contain the traversal logic that walks the field hierarchy. `TreeMapEntityState` implements `EntityState` directly with flat `Map<FieldPath, Object>` storage, bypassing fields entirely.

This means structural operations like vector resizing (`VectorField`), pointer type switching (`PointerField`), and capacity management (`SerializerField`, `ArrayField`) only work with the nested array implementation. Adding new state implementations requires duplicating or reimplementing this logic.

## Goals / Non-Goals

**Goals:**
- Fields operate against a common interface, independent of concrete EntityState implementation
- Both NestedArrayEntityState and TreeMapEntityState share the same field-driven traversal logic
- Clean naming that reflects the nested navigation semantics rather than implementation details

**Non-Goals:**
- Changing S1 entity state handling (`ObjectArrayEntityState`)
- Performance optimization of either state implementation
- Changing the `EntityState` interface itself
- Adding new state implementations beyond the existing two

## Decisions

### 1. Rename `ArrayEntityState` to `NestedEntityState`

The interface describes nested index-based navigation (`sub()`, `isSub()`), not a concrete array. "Nested" accurately describes the behavior both implementations share.

**Alternative**: Keep `ArrayEntityState`. Rejected because the TreeMap implementation has nothing array-like about it — the name would mislead.

### 2. Introduce `S2EntityState` abstract base class

A new abstract class that:
- Extends nothing (is a class)
- Implements `EntityState` and `NestedEntityState`
- Holds `rootField: SerializerField`
- Contains the shared traversal logic for `setValueForFieldPath` and `getValueForFieldPath`
- Declares abstract methods: `copy()`, `fieldPathIterator()`, `capacityChanged()`

The traversal logic is lifted verbatim from `NestedArrayEntityState`, operating on `this` as the root `NestedEntityState`.

**Alternative**: Default methods on `NestedEntityState`. Rejected because the traversal needs `rootField` state, which doesn't belong on an interface.

**Alternative**: Utility class with static methods. Rejected because the traversal is core behavior, not a utility.

### 3. TreeMap prefix views using S2 long field path encoding

`TreeMapEntityState.sub(idx)` returns a lightweight `View` implementing `NestedEntityState`:
- Stores a reference to the root `TreeMapEntityState` and a `long prefix` (S2 long field path format)
- `sub(idx)` creates a new View by extending the prefix via `S2LongFieldPathFormat.down()`/`set()`
- `get(idx)`, `set(idx, value)` build the full key from prefix + idx and operate on the shared map
- `capacity(n, true)` uses sorted map range operations to remove keys beyond the prefix range
- `length()` derives the count from the sorted map's range under the prefix

The AVL tree map is already sorted by field path long comparison, making range operations efficient (O(log n)).

**Alternative**: Mutable cursor that tracks depth. Rejected because `VectorField` calls `state.sub(idx).capacity(n, true)` — a mutable cursor would leave depth in wrong state after the chained call.

**Alternative**: int[] prefix arrays. Rejected because the long encoding is already the native key format and provides natural sorted range queries.

### 4. Rename Field methods

| Old | New |
|---|---|
| `getArrayEntityState(ArrayEntityState, int)` | `getValue(NestedEntityState, int)` |
| `setArrayEntityState(ArrayEntityState, int, int, Object)` | `setValue(NestedEntityState, int, int, Object)` |
| `ensureArrayEntityStateCapacity(ArrayEntityState, int)` | `ensureCapacity(NestedEntityState, int)` |

The old names were verbose and encoded the parameter type in the method name. The new names describe the operation.

## Risks / Trade-offs

- **View object allocation in TreeMap path**: Each `sub()` call creates a small View object (object header + long + reference). For the TreeMap state this adds GC pressure on hot paths. → Mitigation: Views are tiny and short-lived (nursery-collected). NestedArray remains the performance-critical implementation; TreeMap is the debug/validation alternative.

- **`length()` on TreeMap views is O(log n)**: Unlike the O(1) array length, TreeMap needs a range scan. This is called in the traversal loop's capacity check. → Mitigation: The capacity check is `node.length() <= idx` — for TreeMap, this could be optimized by checking key existence directly rather than computing full length. But premature optimization is a non-goal; measure first.

- **Breaking change**: Renaming `ArrayEntityState` and Field methods breaks any external code using these types directly. → Mitigation: This is an internal API. External users interact through `EntityState` and `Entity.getProperty()`, not through the field/state internals.
