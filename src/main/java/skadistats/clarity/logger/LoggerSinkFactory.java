package skadistats.clarity.logger;

import java.util.Map;

public interface LoggerSinkFactory {

    LoggerSink getLoggerSink(Map<String, LoggerSink> sinkMap, String category);

}
