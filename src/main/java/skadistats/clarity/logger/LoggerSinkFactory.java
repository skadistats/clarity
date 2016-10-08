package skadistats.clarity.logger;

public interface LoggerSinkFactory {

    LoggerSink getLoggerSink(String category);

}
