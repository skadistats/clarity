/**
 * Entity tracking and the {@code @OnEntity*} events.
 *
 * <p>{@link skadistats.clarity.processor.entities.Entities} applies the {@code CSVCMsg_PacketEntities} messages of
 * the replay and keeps one persistent {@link skadistats.clarity.model.Entity} per entity, whose state is updated in
 * place. Annotate your processor with {@link skadistats.clarity.processor.entities.UsesEntities} to activate it;
 * look entities up with {@code Entities.getByIndex(int)} or {@code Entities.getByHandle(int)}. Entities can be
 * excluded by class with the runner's {@code withEntityFilter}; they then never appear.
 *
 * <p>Events, in the order they occur for an entity:
 * <ol>
 *   <li>{@link skadistats.clarity.processor.entities.OnEntityCreated}: the entity exists and its state is populated.
 *       If a different entity held the same index, its {@link skadistats.clarity.processor.entities.OnEntityLeft}
 *       (if it was active) and {@link skadistats.clarity.processor.entities.OnEntityDeleted} come first.</li>
 *   <li>{@link skadistats.clarity.processor.entities.OnEntityEntered}: the entity became active (visible to the
 *       client); for a new entity directly after created.</li>
 *   <li>{@link skadistats.clarity.processor.entities.OnEntityPropertyCountChanged}: properties were added or removed;
 *       raised before the accompanying updated event.</li>
 *   <li>{@link skadistats.clarity.processor.entities.OnEntityUpdated}: changed field paths of an existing entity.</li>
 *   <li>{@link skadistats.clarity.processor.entities.OnEntityPropertyChanged}: once per changed field path, for
 *       created and updated entities, filterable by {@code classPattern} and {@code propertyPattern}. It is
 *       dispatched from built-in listeners of created and updated (declared with {@code @Order(1000)}).</li>
 *   <li>{@link skadistats.clarity.processor.entities.OnEntityLeft}: the entity became inactive, or is about to be deleted.</li>
 *   <li>{@link skadistats.clarity.processor.entities.OnEntityDeleted}: the entity ceased to exist.</li>
 * </ol>
 * After all entity events of a packet,
 * {@link skadistats.clarity.processor.entities.OnEntityUpdatesCompleted} fires once.
 *
 * <p>Events are raised after the whole packet has been applied, so {@code Entity.getState()} reflects every change
 * of that packet. After a seek with a {@link skadistats.clarity.processor.runner.ControllableRunner}, events are
 * suppressed during the reset and the net difference is raised when it completes.
 *
 * <pre>{@code
 * @UsesEntities
 * public class Tracker {
 *     @OnEntityPropertyChanged(classPattern = "CDOTA_Unit_Hero_.*", propertyPattern = "m_lifeState")
 *     public void onLifeState(Context ctx, Entity e, FieldPath fp) {
 *         System.out.println(ctx.getTick() + " " + e.getNameForFieldPath(fp) + " = " + e.getPropertyForFieldPath(fp));
 *     }
 * }
 * }</pre>
 */
package skadistats.clarity.processor.entities;
