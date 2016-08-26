package skadistats.clarity.event;

import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UsagePoints {

    private static final Logger log = LoggerFactory.getLogger(UsagePoints.class);

    private static Map<Class<? extends Annotation>, List<UsagePointProvider>> PROVIDERS = new HashMap<>();

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
                List<UsagePointProvider> providersForClass = PROVIDERS.get(usagePointClass);
                if (providersForClass == null) {
                    providersForClass = new LinkedList<>();
                    PROVIDERS.put(usagePointClass, providersForClass);
                }
                providersForClass.add(new UsagePointProvider(usagePointClass, providerClass, provideAnnotation));
            }

            for (List<UsagePointProvider> providersForClass : PROVIDERS.values()) {
                Collections.sort(providersForClass, new Comparator<UsagePointProvider>() {
                    @Override
                    public int compare(UsagePointProvider o1, UsagePointProvider o2) {
                        return o1.getProvidesAnnotation().precedence() - o2.getProvidesAnnotation().precedence();
                    }
                });
            }
        }
    }

    public static List<UsagePointProvider> getProvidersFor(Class<? extends Annotation> usagePointClass) {
        return PROVIDERS.get(usagePointClass);
    }

}
