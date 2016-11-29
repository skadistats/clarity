package skadistats.clarity.logger;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrintfLoggerFactory {

    private static final Map<String, PrintfLogger> loggerCache = new HashMap<>();

    static {
        try {
            ILoggerFactory f = LoggerFactory.getILoggerFactory();
            Class<? extends ILoggerFactory> c = f.getClass();
            if (c.getName().equals("ch.qos.logback.classic.LoggerContext")) {
                List<String> frameworkPackages = (List<String>) c.getMethod("getFrameworkPackages").invoke(f);
                frameworkPackages.add("skadistats.clarity.logger");
            }
        } catch (Exception e) {}
    }

    public static PrintfLogger getLogger(Enum category) {
        return getLogger(category.name());
    }

    public static PrintfLogger getLogger(String name) {
        PrintfLogger logger = loggerCache.get(name);
        if (logger == null) {
            logger = new PrintfLogger(org.slf4j.LoggerFactory.getLogger(name));
            loggerCache.put(name, logger);
        }
        return logger;
    }

}
