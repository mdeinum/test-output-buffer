package com.example.testoutput;

import java.io.PrintStream;
import java.nio.charset.Charset;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Buffers everything written to {@code System.out}/{@code System.err} (including console log output) per test and
 * only prints it when the test fails.
 * <p>
 * Registered by {@link OutputBufferingSessionListener}. Works for every JUnit Platform engine, including Vintage
 * (JUnit 4).
 */
public class OutputBufferingListener implements TestExecutionListener {

    private final BufferConfig config;

    public OutputBufferingListener() {
        this(BufferConfig.fromSystemProperties());
    }

    OutputBufferingListener(BufferConfig config) {
        this.config = config;
    }

    private volatile TestOutputRegistry registry;
    private InterceptingOutputStream out;
    private InterceptingOutputStream err;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private PrintStream installedOut;
    private PrintStream installedErr;

    @Override
    public synchronized void testPlanExecutionStarted(TestPlan testPlan) {
        if (!config.enabled() || registry != null) {
            return;
        }
        originalOut = System.out;
        originalErr = System.err;
        registry = new TestOutputRegistry(config, originalOut, originalErr);
        Charset charset = consoleCharset();
        OutputClassifier classifier = new OutputClassifier(config.loggingPackages());
        boolean route = config.routeToSlf4j() && Slf4jRouter.isAvailable();
        out = new InterceptingOutputStream(Target.OUT, registry, classifier, charset, config.stdoutLevel(), route);
        err = new InterceptingOutputStream(Target.ERR, registry, classifier, charset, config.stderrLevel(), route);
        installedOut = new InterceptingPrintStream(out, charset);
        installedErr = new InterceptingPrintStream(err, charset);
        System.setOut(installedOut);
        System.setErr(installedErr);
        if (route) {
            // initialize the logging framework up front, not concurrently from parallel tests
            Slf4jRouter.initialize();
        }
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        TestOutputRegistry current = registry;
        if (current != null) {
            current.started(testIdentifier.getUniqueId(), testIdentifier.getParentId().orElse(null),
                    testIdentifier.isTest());
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
        TestOutputRegistry current = registry;
        if (current == null) {
            return;
        }
        installedOut.flush();
        installedErr.flush();
        out.completePendingLine();
        err.completePendingLine();
        current.finished(testIdentifier.getUniqueId(), shouldFlush(result));
    }

    @Override
    public synchronized void testPlanExecutionFinished(TestPlan testPlan) {
        if (registry == null) {
            return;
        }
        installedOut.flush();
        installedErr.flush();
        out.completePendingLine();
        err.completePendingLine();
        registry.planFinished();
        // only restore when nobody replaced our streams in the meantime
        if (System.out == installedOut) {
            System.setOut(originalOut);
        }
        if (System.err == installedErr) {
            System.setErr(originalErr);
        }
        registry = null;
    }

    private boolean shouldFlush(TestExecutionResult result) {
        return switch (result.getStatus()) {
            case FAILED -> true;
            case ABORTED -> config.flushOnAborted();
            case SUCCESSFUL -> false;
        };
    }

    private static Charset consoleCharset() {
        String encoding = System.getProperty("stdout.encoding");
        try {
            return encoding != null ? Charset.forName(encoding) : Charset.defaultCharset();
        }
        catch (RuntimeException ex) {
            return Charset.defaultCharset();
        }
    }
}
