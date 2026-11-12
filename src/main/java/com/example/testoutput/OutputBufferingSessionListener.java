package com.example.testoutput;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;
import org.junit.platform.launcher.TestExecutionListener;

/**
 * Entry point, registered through {@code META-INF/services}: active as soon as the jar is on the test classpath.
 * Disable with {@code -Dtestoutput.buffer.enabled=false}.
 * <p>
 * Registers the {@link OutputBufferingListener} on the session's launcher. Preferably the eager variant, which flushes
 * a failed test's output before other listeners (Surefire) report the test as finished, so the output ends up in
 * that test's {@code <system-out>}. It relies on JUnit internals; when it cannot be loaded (e.g. tests running on
 * the module path) the regular listener is used, which still prints to the console.
 */
public class OutputBufferingSessionListener implements LauncherSessionListener {

    static final String EAGER_LISTENER = "org.junit.platform.launcher.core.OutputBufferingEagerListener";

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        BufferConfig config = BufferConfig.fromSystemProperties();
        if (config.enabled()) {
            session.getLauncher().registerTestExecutionListeners(createListener());
        }
    }

    private static TestExecutionListener createListener() {
        try {
            Class<?> eager = Class.forName(EAGER_LISTENER, true, OutputBufferingSessionListener.class.getClassLoader());
            return (TestExecutionListener) eager.getDeclaredConstructor().newInstance();
        }
        catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            return new OutputBufferingListener();
        }
    }
}
