/**
 * Entity property storage.
 *
 * <p>For users, {@link skadistats.clarity.state.EntityState} (obtained from {@code Entity.getState()}) holds the
 * property values of an entity addressed by {@link skadistats.clarity.model.FieldPath}. The parser mutates it in
 * place, so keep a {@code copy()} or a {@link skadistats.clarity.state.StateDelta} if values must survive later
 * updates. {@code EntityState.captureChanged(state, fps, num)} produces a {@code StateDelta}, a sparse immutable
 * snapshot of the given fields that is read through primitive getters. The storage strategy is selected per run on
 * the runner ({@code withS1EntityState}, {@code withS2EntityState}).
 *
 * <p>The other classes in this package ({@code EntityRegistry}, {@code BaselineRegistry}, {@code ClientFrame},
 * {@code StateMutation}, {@code FieldLayout}, {@code PrimitiveType}, {@code SparseStateDelta}) and the {@code s1}
 * and {@code s2} subpackages are parser internals.
 */
package skadistats.clarity.state;
