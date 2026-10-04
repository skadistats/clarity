/**
 * Engine-specific behaviour of the supported games.
 *
 * <p>{@link skadistats.clarity.engine.EngineType} describes the demo file layout, entity handle encoding and packet
 * class lookup of one replay; the runner's {@code getEngineType()} returns it, and its {@code getId()} identifies
 * the {@link skadistats.clarity.model.EngineId}. The other classes here and in the {@code s1} and {@code s2}
 * subpackages implement it per game and are internal.
 */
package skadistats.clarity.engine;
