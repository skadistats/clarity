package org.slf4j.impl;

import org.slf4j.Logger;
import org.slf4j.Marker;

public class LoggerAdapter implements Logger {

    private final skadistats.clarity.logger.Logger delegate;

    public LoggerAdapter(skadistats.clarity.logger.Logger delegate) {
        this.delegate = delegate;
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
        delegate.log(skadistats.clarity.logger.Logger.Level.TRACE, s);
    }

    @Override
    public void trace(String s, Object o) {
        delegate.log(skadistats.clarity.logger.Logger.Level.TRACE, s, o);
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        delegate.log(skadistats.clarity.logger.Logger.Level.TRACE, s, o, o1);
    }

    @Override
    public void trace(String s, Object... objects) {
        delegate.log(skadistats.clarity.logger.Logger.Level.TRACE, s, objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        delegate.trace(throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate.isTraceEnabled();
    }

    @Override
    public void trace(Marker marker, String s) {
        delegate.log(skadistats.clarity.logger.Logger.Level.TRACE, s);
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        delegate.log(skadistats.clarity.logger.Logger.Level.TRACE, s, o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        delegate.log(skadistats.clarity.logger.Logger.Level.TRACE, s, o, o1);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        delegate.log(skadistats.clarity.logger.Logger.Level.TRACE, s, objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        delegate.trace(throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        delegate.log(skadistats.clarity.logger.Logger.Level.DEBUG, s);
    }

    @Override
    public void debug(String s, Object o) {
        delegate.log(skadistats.clarity.logger.Logger.Level.DEBUG, s, o);
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        delegate.log(skadistats.clarity.logger.Logger.Level.DEBUG, s, o, o1);
    }

    @Override
    public void debug(String s, Object... objects) {
        delegate.log(skadistats.clarity.logger.Logger.Level.DEBUG, s, objects);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        delegate.logException(skadistats.clarity.logger.Logger.Level.DEBUG, throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return delegate.isDebugEnabled();
    }

    @Override
    public void debug(Marker marker, String s) {
        delegate.log(skadistats.clarity.logger.Logger.Level.DEBUG, s);
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        delegate.log(skadistats.clarity.logger.Logger.Level.DEBUG, s, o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        delegate.log(skadistats.clarity.logger.Logger.Level.DEBUG, s, o, o1);
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        delegate.log(skadistats.clarity.logger.Logger.Level.DEBUG, s, objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        delegate.logException(skadistats.clarity.logger.Logger.Level.DEBUG, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        delegate.log(skadistats.clarity.logger.Logger.Level.INFO, s);
    }

    @Override
    public void info(String s, Object o) {
        delegate.log(skadistats.clarity.logger.Logger.Level.INFO, s, o);
    }

    @Override
    public void info(String s, Object o, Object o1) {
        delegate.log(skadistats.clarity.logger.Logger.Level.INFO, s, o, o1);
    }

    @Override
    public void info(String s, Object... objects) {
        delegate.log(skadistats.clarity.logger.Logger.Level.INFO, s, objects);
    }

    @Override
    public void info(String s, Throwable throwable) {
        delegate.logException(skadistats.clarity.logger.Logger.Level.INFO, throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return delegate.isInfoEnabled();
    }

    @Override
    public void info(Marker marker, String s) {
        delegate.log(skadistats.clarity.logger.Logger.Level.INFO, s);
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        delegate.log(skadistats.clarity.logger.Logger.Level.INFO, s, o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        delegate.log(skadistats.clarity.logger.Logger.Level.INFO, s, o, o1);
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        delegate.log(skadistats.clarity.logger.Logger.Level.INFO, s, objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        delegate.logException(skadistats.clarity.logger.Logger.Level.INFO, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        delegate.log(skadistats.clarity.logger.Logger.Level.WARN, s);
    }

    @Override
    public void warn(String s, Object o) {
        delegate.log(skadistats.clarity.logger.Logger.Level.WARN, s, o);
    }

    @Override
    public void warn(String s, Object... objects) {
        delegate.log(skadistats.clarity.logger.Logger.Level.WARN, s, objects);
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        delegate.log(skadistats.clarity.logger.Logger.Level.WARN, s, o, o1);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        delegate.logException(skadistats.clarity.logger.Logger.Level.WARN, throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return delegate.isWarnEnabled();
    }

    @Override
    public void warn(Marker marker, String s) {
        delegate.log(skadistats.clarity.logger.Logger.Level.WARN, s);
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        delegate.log(skadistats.clarity.logger.Logger.Level.WARN, s, o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        delegate.log(skadistats.clarity.logger.Logger.Level.WARN, s, o, o1);
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        delegate.log(skadistats.clarity.logger.Logger.Level.WARN, s, objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        delegate.logException(skadistats.clarity.logger.Logger.Level.WARN, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        delegate.log(skadistats.clarity.logger.Logger.Level.ERROR, s);
    }

    @Override
    public void error(String s, Object o) {
        delegate.log(skadistats.clarity.logger.Logger.Level.ERROR, s, o);
    }

    @Override
    public void error(String s, Object o, Object o1) {
        delegate.log(skadistats.clarity.logger.Logger.Level.ERROR, s, o, o1);
    }

    @Override
    public void error(String s, Object... objects) {
        delegate.log(skadistats.clarity.logger.Logger.Level.ERROR, s, objects);
    }

    @Override
    public void error(String s, Throwable throwable) {
        delegate.logException(skadistats.clarity.logger.Logger.Level.ERROR, throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return delegate.isErrorEnabled();
    }

    @Override
    public void error(Marker marker, String s) {
        delegate.log(skadistats.clarity.logger.Logger.Level.ERROR, s);
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        delegate.log(skadistats.clarity.logger.Logger.Level.ERROR, s, o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        delegate.log(skadistats.clarity.logger.Logger.Level.ERROR, s, o, o1);
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        delegate.log(skadistats.clarity.logger.Logger.Level.ERROR, s, objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        delegate.logException(skadistats.clarity.logger.Logger.Level.ERROR, throwable);
    }
}
