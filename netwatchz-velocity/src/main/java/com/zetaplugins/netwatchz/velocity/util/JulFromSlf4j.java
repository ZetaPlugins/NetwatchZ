package com.zetaplugins.netwatchz.velocity.util;

import java.util.logging.Logger;

/**
 * A simple adapter to use an SLF4J logger as a java.util.logging.Logger.
 * This is because the common module uses java.util.logging.Logger.
 */
public class JulFromSlf4j extends Logger {
    private final org.slf4j.Logger slf4jLogger;

    public JulFromSlf4j(String name, org.slf4j.Logger slf4jLogger) {
        super(name, null);
        this.slf4jLogger = slf4jLogger;
    }

    @Override
    public void info(String msg) {
        slf4jLogger.info(msg);
    }

    @Override
    public void warning(String msg) {
        slf4jLogger.warn(msg);
    }

    @Override
    public void severe(String msg) {
        slf4jLogger.error(msg);
    }

    @Override
    public void fine(String msg) {
        slf4jLogger.debug(msg);
    }
}
