package skadistats.clarity.event;

import skadistats.clarity.model.EngineId;
import skadistats.clarity.processor.runner.Runner;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a processor class as providing one or more event or feature annotations.
 * <p>
 * When a processor uses an annotation, the execution model instantiates a processor that provides it.
 * Provider classes are found through {@code META-INF/clarity/providers.txt}, which the
 * annotation processor generates from the {@code @Provides} annotations at compile time.
 * <p>
 * If several providers offer an annotation, providers whose {@link #engine()} or
 * {@link #runnerClass()} restriction does not match the current runner are skipped. An already
 * instantiated matching provider is used; otherwise the one with the lowest {@link #precedence()}.
 * <p>
 * Example:
 * <pre>{@code
 * @Provides(value = {OnMyEvent.class}, engine = {EngineId.DOTA_S2})
 * public class MyProvider { ... }
 * }</pre>
 *
 * @see skadistats.clarity.event.UsagePoints
 * @see skadistats.clarity.processor.runner.ExecutionModel
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface Provides {

    /**
     * Event or feature annotations provided by this processor.
     * Each annotation must be marked with {@code @UsagePointMarker}.
     */
    Class<? extends Annotation>[] value();

    /**
     * Engine types for which this provider is usable.
     * If empty, it is usable for all engine types.
     */
    EngineId[] engine() default {};

    /**
     * Runner classes this provider is usable with (the current runner must be an instance of one of them).
     * If empty, it is usable with all runners.
     */
    Class<? extends Runner>[] runnerClass() default {};

    /**
     * When multiple usable providers offer the same annotation and none is instantiated yet,
     * the one with the lowest value is chosen. Default is 0.
     */
    int precedence() default 0;

}
