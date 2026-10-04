/**
 * Value types that the events hand to your code.
 *
 * <p>{@link skadistats.clarity.model.Entity} is a live entity of a {@link skadistats.clarity.model.DTClass}; read its
 * properties by name or by {@link skadistats.clarity.model.FieldPath}, resolving the path once with
 * {@code getFieldPathForName} when reading repeatedly. A {@code FieldPath} identifies one property of an entity's
 * {@link skadistats.clarity.state.EntityState}; paths are immutable and comparable with {@code equals}. Vector-typed
 * properties are {@link skadistats.clarity.model.Vector}s.
 *
 * <p>{@link skadistats.clarity.model.StringTable}, {@link skadistats.clarity.model.GameEvent} with its
 * {@link skadistats.clarity.model.GameEventDescriptor}, and {@link skadistats.clarity.model.CombatLogEntry} carry
 * the corresponding replay data. {@link skadistats.clarity.model.EngineId} names the game and engine of a replay.
 * The {@code s1}, {@code s2} and {@code cs} subpackages hold engine-specific implementations.
 */
package skadistats.clarity.model;
