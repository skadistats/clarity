/**
 * Game events and the Dota 2 combat log.
 *
 * <p>{@link skadistats.clarity.processor.gameevents.OnGameEventDescriptor} reports the definition of each game event
 * type and {@link skadistats.clarity.processor.gameevents.OnGameEvent} each occurrence as a
 * {@link skadistats.clarity.model.GameEvent}. {@link skadistats.clarity.processor.gameevents.OnCombatLogEntry}
 * delivers Dota 2 combat log entries as {@link skadistats.clarity.model.CombatLogEntry}, for both Source 1 and
 * Source 2 replays, at the end of the tick in which they were recorded.
 */
package skadistats.clarity.processor.gameevents;
