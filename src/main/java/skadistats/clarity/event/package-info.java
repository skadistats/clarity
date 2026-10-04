/**
 * Annotation-driven event dispatch and processor wiring.
 *
 * <h2>Concepts</h2>
 *
 * <p>A <em>usage point</em> is an annotation that carries {@link UsagePointMarker}. A processor
 * class declares usage points by annotating itself or its methods. The {@link UsagePointType}
 * of the marker decides what it means:
 * <ul>
 *   <li>{@code EVENT_LISTENER}: a method annotated with the event annotation (for example
 *       {@code @OnEntityCreated}) receives that event.</li>
 *   <li>{@code INITIALIZER}: a method annotated with {@link Initializer} is called once for each
 *       usage point of the given annotation, typically to install a filter.</li>
 *   <li>{@code FEATURE}: an annotation (usually on a class) that only declares a requirement;
 *       the runner instantiates a processor that {@link Provides} it.</li>
 * </ul>
 *
 * <p>A processor that implements usage points is annotated with {@link Provides}. When a
 * processor requires a usage point, the execution model picks a matching provider
 * (see {@link Provides}) and instantiates it. After instantiation the execution model binds
 * listener methods, fills {@link Insert} and {@link InsertEvent} fields, and calls initializers.
 *
 * <h2>Event annotations</h2>
 *
 * <p>An event annotation is marked {@code @UsagePointMarker(UsagePointType.EVENT_LISTENER)} and,
 * to get a generated dispatcher, {@link GenerateEvent}. By convention it contains nested types:
 * <ul>
 *   <li>{@code Listener}: interface with one method. Handler methods take an optional leading
 *       {@link skadistats.clarity.processor.runner.Context} followed by exactly that method's parameters.</li>
 *   <li>{@code Filter} (optional): interface with one method taking the same parameters and returning
 *       {@code boolean}. The generated {@code raise()} skips a listener whose filter returns false.</li>
 *   <li>{@code Event}: interface extending {@link EventBase} with {@code raise(...)}; the generated
 *       {@code *_Event} class implements it.</li>
 * </ul>
 *
 * <h2>Example</h2>
 *
 * <p>Defining an event:
 * <pre>{@code
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target(ElementType.METHOD)
 * @UsagePointMarker(UsagePointType.EVENT_LISTENER)
 * @GenerateEvent
 * public @interface OnMyEvent {
 *     interface Listener {
 *         void invoke(String message);
 *     }
 *     interface Event extends EventBase {
 *         void raise(String message);
 *     }
 * }
 * }</pre>
 *
 * <p>Raising it from a provider:
 * <pre>{@code
 * @Provides(OnMyEvent.class)
 * public class MyProvider {
 *     @InsertEvent
 *     private OnMyEvent.Event myEvent;
 *
 *     void doSomething() {
 *         if (myEvent.isListenedTo()) {
 *             myEvent.raise("hello");
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>Listening, with the event annotation on the method:
 * <pre>{@code
 * public class MyListener {
 *     @OnMyEvent
 *     @Order(100)
 *     public void onMyEvent(String message) {
 *         System.out.println(message);
 *     }
 * }
 * }</pre>
 *
 * <h2>Related classes</h2>
 *
 * <ul>
 *   <li>{@link Provides}: registers a processor as provider of usage points.</li>
 *   <li>{@link UsagePointMarker}, {@link UsagePointType}: define usage point annotations.</li>
 *   <li>{@link Event}, {@link EventListener}: a runtime event and its bound listeners.</li>
 *   <li>{@link Insert}, {@link InsertEvent}: field injection.</li>
 *   <li>{@link Initializer}: per-usage-point setup.</li>
 *   <li>{@link Order}: listener order.</li>
 *   <li>{@link GenerateEvent}: generation of the typed event class.</li>
 * </ul>
 *
 * @see skadistats.clarity.processor.runner.ExecutionModel
 */
package skadistats.clarity.event;
