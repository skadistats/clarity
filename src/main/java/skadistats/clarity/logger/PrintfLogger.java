package skadistats.clarity.logger;

import org.slf4j.Logger;
import org.slf4j.Marker;

public class PrintfLogger implements Logger {

    private final Logger delegate;

    public PrintfLogger(Logger delegate) {
        this.delegate = delegate;
    }

    private String format(String fmt, Object... params) {
        return String.format(fmt, params);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        if (delegate.isTraceEnabled()) {
            delegate.trace(s);
        }
    }

    @Override
    public void trace(String s, Object o) {
        if (delegate.isTraceEnabled()) {
            delegate.trace(format(s, o));
        }
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        if (delegate.isTraceEnabled()) {
            delegate.trace(format(s, o, o1));
        }
    }

    @Override
    public void trace(String s, Object... objects) {
        if (delegate.isTraceEnabled()) {
            delegate.trace(format(s, objects));
        }
    }

    @Override
    public void trace(String s, Throwable throwable) {
        delegate.trace(s, throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String s) {
        if (delegate.isTraceEnabled(marker)) {
            delegate.trace(marker, s);
        }
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        if (delegate.isTraceEnabled(marker)) {
            delegate.trace(marker, format(s, o));
        }
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        if (delegate.isTraceEnabled(marker)) {
            delegate.trace(marker, format(s, o, o1));
        }
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        if (delegate.isTraceEnabled(marker)) {
            delegate.trace(marker, format(s, objects));
        }
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        delegate.trace(marker, s, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        delegate.debug(s);
    }

    @Override
    public void debug(String s, Object o) {
        if (delegate.isDebugEnabled()) {
            delegate.debug(format(s, o));
        }
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        if (delegate.isDebugEnabled()) {
            delegate.debug(format(s, o, o1));
        }
    }

    @Override
    public void debug(String s, Object... objects) {
        if (delegate.isDebugEnabled()) {
            delegate.debug(format(s, objects));
        }
    }

    @Override
    public void debug(String s, Throwable throwable) {
        delegate.debug(s, throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return delegate.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String s) {
        delegate.debug(marker, s);
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        if (delegate.isDebugEnabled(marker)) {
            delegate.debug(marker, format(s, o));
        }
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        if (delegate.isDebugEnabled(marker)) {
            delegate.debug(marker, format(s, o, o1));
        }
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        if (delegate.isDebugEnabled(marker)) {
            delegate.debug(marker, format(s, objects));
        }
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        if (delegate.isDebugEnabled(marker)) {
            delegate.debug(marker, s, throwable);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        delegate.info(s);
    }

    @Override
    public void info(String s, Object o) {
        if (delegate.isInfoEnabled()) {
            delegate.info(format(s, o));
        }
    }

    @Override
    public void info(String s, Object o, Object o1) {
        if (delegate.isInfoEnabled()) {
            delegate.info(format(s, o, o1));
        }
    }

    @Override
    public void info(String s, Object... objects) {
        if (delegate.isInfoEnabled()) {
            delegate.info(format(s, objects));
        }
    }

    @Override
    public void info(String s, Throwable throwable) {
        delegate.info(s, throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return delegate.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        if (delegate.isInfoEnabled(marker)) {
            delegate.info(marker, s);
        }
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        if (delegate.isInfoEnabled(marker)) {
            delegate.info(marker, format(s, o));
        }
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        if (delegate.isInfoEnabled(marker)) {
            delegate.info(marker, format(s, o, o1));
        }
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        if (delegate.isInfoEnabled(marker)) {
            delegate.info(marker, format(s, objects));
        }
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        delegate.info(marker, s, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        delegate.warn(s);
    }

    @Override
    public void warn(String s, Object o) {
        if (delegate.isWarnEnabled()) {
            delegate.warn(format(s, o));
        }
    }

    @Override
    public void warn(String s, Object... objects) {
        if (delegate.isWarnEnabled()) {
            delegate.warn(format(s, objects));
        }
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        if (delegate.isWarnEnabled()) {
            delegate.warn(format(s, o, o1));
        }
    }

    @Override
    public void warn(String s, Throwable throwable) {
        delegate.warn(s, throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return delegate.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String s) {
        if (delegate.isWarnEnabled(marker)) {
            delegate.warn(marker, s);
        }
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        if (delegate.isWarnEnabled(marker)) {
            delegate.warn(marker, format(s, o));
        }
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        if (delegate.isWarnEnabled(marker)) {
            delegate.warn(marker, format(s, o, o1));
        }
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        if (delegate.isWarnEnabled(marker)) {
            delegate.warn(marker, format(s, objects));
        }
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        delegate.warn(marker, s, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        delegate.error(s);
    }

    @Override
    public void error(String s, Object o) {
        if (delegate.isErrorEnabled()) {
            delegate.error(format(s, o));
        }
    }

    @Override
    public void error(String s, Object o, Object o1) {
        if (delegate.isErrorEnabled()) {
            delegate.error(format(s, o, o1));
        }
    }

    @Override
    public void error(String s, Object... objects) {
        if (delegate.isErrorEnabled()) {
            delegate.error(format(s, objects));
        }
    }

    @Override
    public void error(String s, Throwable throwable) {
        delegate.error(s, throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return delegate.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        delegate.error(marker, s);
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        if (delegate.isErrorEnabled(marker)) {
            delegate.error(marker, format(s, o));
        }
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        if (delegate.isErrorEnabled(marker)) {
            delegate.error(marker, format(s, o, o1));
        }
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        if (delegate.isErrorEnabled(marker)) {
            delegate.error(marker, format(s, objects));
        }
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        delegate.error(marker, s, throwable);
    }
}
