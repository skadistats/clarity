package skadistats.clarity.logger;

public class Logger {

    public enum Level { DEBUG, INFO, WARN, ERROR }

    private final LoggerSink sink;
    private final String category;

    private Level level;

    public Logger(String category, LoggerSink sink, Level level) {
        this.sink = sink;
        this.category = category;
        this.level = level;
    }

    public void log(Level level, String format, Object... parameters) {
        if (level.compareTo(this.level) >= 0) {
            sink.log(String.format(
                    "[%6d] %s",
                    (System.nanoTime() - Logging.START_TIME) / 1000000L,
                    String.format(format, parameters)
            ));
        }
    }

    public void debug(String format, Object... parameters) {
        log(Level.DEBUG, format, parameters);
    }

    public void info(String format, Object... parameters) {
        log(Level.INFO, format, parameters);
    }

    public void warn(String format, Object... parameters) {
        log(Level.WARN, format, parameters);
    }

    public void error(String format, Object... parameters) {
        log(Level.ERROR, format, parameters);
    }

    public boolean isDebugEnabled() {
        return Level.DEBUG.compareTo(this.level) >= 0;
    }

}
