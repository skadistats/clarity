package skadistats.clarity.logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Logging {

    public static final long START_TIME = System.nanoTime();

    public static Logger.Level LEVEL = Logger.Level.INFO;

    private static final Map<String, Logger> LOGGER_MAP = new ConcurrentHashMap<>();

    public static LoggerSinkFactory SINK_FACTORY = new LoggerSinkFactory() {
        @Override
        public LoggerSink getLoggerSink(String category) {
            return new LoggerSink() {
                @Override
                public void log(String message) {
                    System.out.println(message);
                }
            };
        }
    } ;

    public static Logger getLogger(String category) {
        Logger logger = LOGGER_MAP.get(category);
        if (logger == null) {
            logger = new Logger(category, SINK_FACTORY.getLoggerSink(category), LEVEL);
            LOGGER_MAP.put(category, logger);
        }
        return logger;
    }

    public static Logger getLogger(Enum category) {
        return getLogger(category.name());
    }

}
