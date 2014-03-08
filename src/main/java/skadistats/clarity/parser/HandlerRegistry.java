package skadistats.clarity.parser;

import java.util.HashMap;
import java.util.Map;

import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.match.Match;
import skadistats.clarity.parser.handler.DemFileHeaderHandler;

import com.google.protobuf.GeneratedMessage;

public class HandlerRegistry {

    private static final Logger log = LoggerFactory.getLogger(HandlerRegistry.class);
    
    private static final Map<Class<?>, Handler<?>> H;
    
    static {
        H = new HashMap<Class<?>, Handler<?>>();
        registerHandlersFromPackage(DemFileHeaderHandler.class.getPackage().getName());
    }
    
    public static void registerHandlersFromPackage(String packageName) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setUrls(ClasspathHelper.forPackage(packageName))
            .setScanners(new TypeAnnotationsScanner())
            );

        for (Class<?> clazz : reflections.getTypesAnnotatedWith(RegisterHandler.class)) {
            RegisterHandler mb = clazz.getAnnotation(RegisterHandler.class);
            try {
                H.put(mb.value(), (Handler<?>) clazz.newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public static <T> void apply(int peekTick, T message, Match match) {
        @SuppressWarnings("unchecked")
        Handler<T> h = (Handler<T>) H.get(message.getClass());
        if (h != null) {
            h.apply(peekTick, message, match);
        } else {
            GeneratedMessage gm = (GeneratedMessage)message;
            log.trace("unable to apply message of type {}", gm.getDescriptorForType().getName());
        }
    }

}
