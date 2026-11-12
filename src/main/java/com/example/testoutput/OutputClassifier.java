package com.example.testoutput;

import java.util.List;

/**
 * Determines who wrote to the console: a logging framework (already formatted output) or application/test code
 * calling {@code System.out}/{@code System.err} directly.
 */
final class OutputClassifier {

    /** Name used when no caller can be determined. */
    static final String UNKNOWN_CALLER = "STDOUT";

    private static final int MAX_DEPTH = 40;

    private static final StackWalker WALKER = StackWalker.getInstance();

    private final List<String> loggingPackages;

    OutputClassifier(List<String> loggingPackages) {
        this.loggingPackages = loggingPackages;
    }

    /**
     * Returns {@code null} when the write originates from a logging framework, otherwise the class name of the
     * code that wrote to the console.
     */
    String directCaller() {
        String caller = WALKER.walk(frames -> frames.limit(MAX_DEPTH)
                .map(StackWalker.StackFrame::getClassName)
                .filter(className -> !isInfrastructure(className))
                .findFirst()
                .orElse(UNKNOWN_CALLER));
        return isLogging(caller) ? null : caller;
    }

    private boolean isLogging(String className) {
        for (String prefix : loggingPackages) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInfrastructure(String className) {
        return className.startsWith("java.")
                || className.startsWith("jdk.")
                || className.startsWith("sun.")
                || className.startsWith(OutputClassifier.class.getPackageName() + ".");
    }
}
