/**
 * Entity class definitions (send tables on Source 1, serializers on Source 2).
 *
 * <p>Declare {@link skadistats.clarity.processor.sendtables.UsesDTClasses} to get the
 * {@link skadistats.clarity.processor.sendtables.DTClasses} registry, which looks up a
 * {@link skadistats.clarity.model.DTClass} by class id or DT name.
 * {@link skadistats.clarity.processor.sendtables.OnDTClass} fires for each class and
 * {@link skadistats.clarity.processor.sendtables.OnDTClassesComplete} once all are loaded, which is the earliest
 * point at which entity classes can be resolved. The emitter and field generator classes are internals.
 */
package skadistats.clarity.processor.sendtables;
