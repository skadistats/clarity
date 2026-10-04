/**
 * Entry point of the Clarity replay parser for Dota 2, CS:GO, CS2 and Deadlock demos.
 *
 * <p>{@link skadistats.clarity.Clarity} offers static helpers that read the file header, the file info or
 * Dota 2 match metadata without parsing the whole replay. To parse a replay, create a runner from
 * {@link skadistats.clarity.processor.runner} and pass it your processor objects.
 *
 * <p>Errors raised by the parser are reported as the unchecked {@link skadistats.clarity.ClarityException}.
 * Exceptions thrown by your own event listeners go to a {@link skadistats.clarity.ClarityExceptionHandler};
 * the default handler rethrows them, which ends the run.
 */
package skadistats.clarity;
