package skadistats.clarity.event;

import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.engine.EngineType;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class UsagePoints {

    private static final Logger log = LoggerFactory.getLogger(UsagePoints.class);

    private static Map<EngineType, Map<Class<? extends Annotation>, UsagePointProvider>> PROVIDERS = new HashMap<>();

    static {
        for (EngineType et : EngineType.values()) {
            PROVIDERS.put(et, new HashMap<Class<? extends Annotation>, UsagePointProvider>());
        }

        for (Class<?> providerClass : ClassIndex.getAnnotated(Provides.class)) {
            log.debug("provider found on ClassIndex: {}", providerClass.getName());
            Provides provideAnnotation = providerClass.getAnnotation(Provides.class);
            if (provideAnnotation == null) {
                // ClassIndex does not reflect real class. Can sometimes happen when working in the IDE.
                continue;
            }
            EngineType[] engineTypes = provideAnnotation.engine();
            if (engineTypes.length == 0) {
                engineTypes = EngineType.values();
            }

            for (EngineType et : engineTypes) {
                Map<Class<? extends Annotation>, UsagePointProvider> providersForEngine = PROVIDERS.get(et);
                for (Class<? extends Annotation> usagePointClass : provideAnnotation.value()) {
                    if (!usagePointClass.isAnnotationPresent(UsagePointMarker.class)) {
                        throw new RuntimeException(String.format("Class %s provides %s, which is not marked as a usage point.", providerClass.getName(), usagePointClass.getName()));
                    }
                    if (providersForEngine.containsKey(usagePointClass)) {
                        log.warn(
                            "ignoring duplicate provider for usage point {}/{} found in {}, already provided by {}",
                            et,
                            usagePointClass.getName(), providerClass.getName(),
                            providersForEngine.get(usagePointClass).getProviderClass().getName()
                        );
                    }
                    providersForEngine.put(usagePointClass, new UsagePointProvider(usagePointClass, providerClass));
                }
            }
        }
    }

    public static UsagePointProvider getProviderFor(Class<? extends Annotation> usagePointClass, EngineType engineType) {
        return PROVIDERS.get(engineType).get(usagePointClass);
    }

}
