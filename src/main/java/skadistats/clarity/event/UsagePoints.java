package skadistats.clarity.event;

import org.slf4j.Logger;
import skadistats.clarity.ClarityException;
import skadistats.clarity.LogChannel;
import skadistats.clarity.logger.PrintfLoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UsagePoints {

    private static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.executionModel);

    private static final Map<Class<? extends Annotation>, List<UsagePointProvider>> PROVIDERS = new HashMap<>();

    static {
        try {
            var resources = UsagePoints.class.getClassLoader().getResources("META-INF/clarity/providers.txt");
            while (resources.hasMoreElements()) {
                var url = resources.nextElement();
                try (var reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        Class<?> providerClass;
                        try {
                            providerClass = Class.forName(line);
                        } catch (ClassNotFoundException e) {
                            log.debug("provider class not found (IDE inconsistency?): %s", line);
                            continue;
                        }
                        log.debug("provider found: %s", providerClass.getName());
                        var provideAnnotation = providerClass.getAnnotation(Provides.class);
                        if (provideAnnotation == null) {
                            continue;
                        }
                        registerProvider(providerClass, provideAnnotation);
                    }
                }
            }
        } catch (Exception e) {
            throw new ClarityException("Failed to load META-INF/clarity/providers.txt", e);
        }
    }

    private static void registerProvider(Class<?> providerClass, Provides provideAnnotation) {
        for (var usagePointClass : provideAnnotation.value()) {
            if (!usagePointClass.isAnnotationPresent(UsagePointMarker.class)) {
                throw new ClarityException("Class %s provides %s, which is not marked as a usage point.", providerClass.getName(), usagePointClass.getName());
            }
            var providersForClass = PROVIDERS.get(usagePointClass);
            if (providersForClass == null) {
                providersForClass = new LinkedList<>();
                PROVIDERS.put(usagePointClass, providersForClass);
            }
            providersForClass.add(new UsagePointProvider(usagePointClass, providerClass, provideAnnotation));
        }

        for (var providersForClass : PROVIDERS.values()) {
            Collections.sort(providersForClass, Comparator.comparingInt(o -> o.getProvidesAnnotation().precedence()));
        }
    }

    public static List<UsagePointProvider> getProvidersFor(Class<? extends Annotation> usagePointClass) {
        return PROVIDERS.get(usagePointClass);
    }

}
