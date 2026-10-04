package skadistats.clarity.event;

import java.lang.annotation.*;

/**
 * Marks a method that is called once for each usage point (listener or feature) of the given
 * annotation, after listeners are bound and fields injected. Providers use it to read the
 * annotation's attributes and install a filter.
 * <p>
 * The method takes the usage point as its single argument ({@link EventListener} for event
 * annotations; {@link UsagePoint} for features), optionally preceded by a
 * {@link skadistats.clarity.processor.runner.Context}. Only one initializer per annotation is
 * used; a duplicate is ignored with a warning.
 * <p>
 * Example (simplified from {@code Entities}):
 * <pre>{@code
 * @Initializer(OnEntityCreated.class)
 * public void initOnEntityCreated(final EventListener<OnEntityCreated> listener) {
 *     var pattern = Pattern.compile(listener.getAnnotation().classPattern());
 *     listener.setFilter((OnEntityCreated.Filter) e -> pattern.matcher(e.getDtClass().getDtName()).matches());
 * }
 * }</pre>
 *
 * @see UsagePointType#INITIALIZER
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.INITIALIZER)
public @interface Initializer {
    /**
     * The annotation whose usage points this method initializes.
     */
    Class<? extends Annotation> value();
}
