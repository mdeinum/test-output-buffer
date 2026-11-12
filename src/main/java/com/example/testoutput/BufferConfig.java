package com.example.testoutput;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Configuration read once from system properties.
 */
record BufferConfig(
        boolean enabled,
        String stdoutLevel,
        String stderrLevel,
        boolean routeToSlf4j,
        List<String> loggingPackages,
        int maxBytesPerTest,
        boolean flushOnAborted) {

    static final String PREFIX = "testoutput.buffer.";

    private static final String DEFAULT_LOGGING_PACKAGES =
            "ch.qos.logback.,org.slf4j.simple.,org.slf4j.impl.SimpleLogger,org.apache.logging.log4j.";

    static BufferConfig fromSystemProperties() {
        return new BufferConfig(
                Boolean.parseBoolean(property("enabled", "true")),
                property("stdout.level", "INFO").toUpperCase(Locale.ROOT),
                property("stderr.level", "ERROR").toUpperCase(Locale.ROOT),
                Boolean.parseBoolean(property("route-to-slf4j", "true")),
                Arrays.stream(property("logging-packages", DEFAULT_LOGGING_PACKAGES).split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList(),
                Integer.parseInt(property("max-bytes-per-test", String.valueOf(8 * 1024 * 1024))),
                Boolean.parseBoolean(property("flush-on-aborted", "false")));
    }

    private static String property(String name, String defaultValue) {
        return System.getProperty(PREFIX + name, defaultValue);
    }
}
