package com.example.testoutput;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs a captured {@code System.out}/{@code System.err} line through SLF4J. Only loaded when SLF4J is on the
 * classpath.
 */
final class Slf4jRouter {

    private Slf4jRouter() {
    }

    static boolean isAvailable() {
        try {
            Class.forName("org.slf4j.LoggerFactory", false, Slf4jRouter.class.getClassLoader());
            return true;
        }
        catch (ClassNotFoundException | LinkageError ex) {
            return false;
        }
    }

    static void initialize() {
        try {
            LoggerFactory.getILoggerFactory();
        }
        catch (RuntimeException | LinkageError ex) {
            // routing falls back to buffering raw lines
        }
    }

    /**
     * Returns {@code false} when the line was not logged (no SLF4J provider bound, or the level is disabled), in
     * which case the caller buffers the raw line so nothing is lost.
     */
    static boolean log(String loggerName, String level, String line) {
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        String factoryName = factory.getClass().getName();
        if (factoryName.contains("NOP") || factoryName.contains("Substitute")) {
            return false;
        }
        Logger logger = factory.getLogger(loggerName);
        switch (level) {
            case "TRACE" -> {
                if (!logger.isTraceEnabled()) return false;
                logger.trace(line);
            }
            case "DEBUG" -> {
                if (!logger.isDebugEnabled()) return false;
                logger.debug(line);
            }
            case "WARN" -> {
                if (!logger.isWarnEnabled()) return false;
                logger.warn(line);
            }
            case "ERROR" -> {
                if (!logger.isErrorEnabled()) return false;
                logger.error(line);
            }
            default -> {
                if (!logger.isInfoEnabled()) return false;
                logger.info(line);
            }
        }
        return true;
    }
}
