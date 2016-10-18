package skadistats.clarity.logger;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger {

    public enum Level { TRACE, DEBUG, INFO, WARN, ERROR }

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
            String prefix = String.format(
                    "[%9.2fms] %-15s | ",
                    (float)(System.nanoTime() - Logging.START_TIME) / 1000000.0f,
                    category
            );

            String payload = String.format(format, parameters);
            payload = payload.replaceAll("\\n$", "");
            if (!payload.isEmpty()) {
                sink.log(prefix + payload.replaceAll("(?<=\n)", String.format("%" + prefix.length() + "s", " ")));
            }
        }
    }

    public void logException(Level level, Throwable e) {
        StringWriter w = new StringWriter();
        e.printStackTrace(new PrintWriter(w));
        log(level, w.toString());
    }

    public void trace(String format, Object... parameters) {
        log(Level.TRACE, format, parameters);
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

    public void trace(Throwable e) {
        logException(Level.TRACE, e);
    }

    public void debug(Throwable e) {
        logException(Level.DEBUG, e);
    }

    public void info(Throwable e) {
        logException(Level.INFO, e);
    }

    public void warn(Throwable e) {
        logException(Level.WARN, e);
    }

    public void error(Throwable e) {
        logException(Level.ERROR, e);
    }

    public boolean isLevelEnabled(Level level) {
        return level.compareTo(this.level) >= 0;
    }

    public boolean isTraceEnabled() {
        return isLevelEnabled(Level.TRACE);
    }

    public boolean isDebugEnabled() {
        return isLevelEnabled(Level.DEBUG);
    }

    public boolean isInfoEnabled() {
        return isLevelEnabled(Level.INFO);
    }

    public boolean isWarnEnabled() {
        return isLevelEnabled(Level.WARN);
    }

    public boolean isErrorEnabled() {
        return isLevelEnabled(Level.ERROR);
    }

    public String getName() {
        return category;
    }

}
