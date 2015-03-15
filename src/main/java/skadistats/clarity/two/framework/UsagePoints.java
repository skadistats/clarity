package skadistats.clarity.two.framework;

import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.two.framework.annotation.Provides;
import skadistats.clarity.two.framework.annotation.UsagePointMarker;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class UsagePoints {

    private static final Logger log = LoggerFactory.getLogger(UsagePoints.class);

    private static Map<Class<? extends Annotation>, UsagePointProvider> PROVIDERS = new HashMap<>();

    static {
        for (Class<?> providerClass : ClassIndex.getAnnotated(Provides.class)) {
            log.debug("provider found on ClassIndex: {}", providerClass.getName());
            Provides provideAnnotation = providerClass.getAnnotation(Provides.class);
            if (provideAnnotation == null) {
                // ClassIndex does not reflect real class. Can sometimes happen when working in the IDE.
                continue;
            }
            for (Class<? extends Annotation> usagePointClass : provideAnnotation.value()) {
                if (!usagePointClass.isAnnotationPresent(UsagePointMarker.class)) {
                    throw new RuntimeException(String.format("Class %s provides %s, which is not marked as a usage point.", providerClass.getName(), usagePointClass.getName()));
                }
                if (PROVIDERS.containsKey(usagePointClass)) {
                    log.warn("ignoring duplicate provider for usage point {} found in {}, already provided by {}", usagePointClass.getName(), providerClass.getName(), PROVIDERS.get(usagePointClass).getProviderClass().getName());
                }
                PROVIDERS.put(usagePointClass, new UsagePointProvider(usagePointClass, providerClass));
            }
        }
    }

    public static UsagePointProvider getProvidersFor(Class<? extends Annotation> usagePointClass) {
        return PROVIDERS.get(usagePointClass);
    }

}
