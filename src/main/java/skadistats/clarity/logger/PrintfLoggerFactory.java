package skadistats.clarity.logger;

import java.util.HashMap;
import java.util.Map;

public class PrintfLoggerFactory {

    private static final Map<String, PrintfLogger> loggerCache = new HashMap<>();

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
