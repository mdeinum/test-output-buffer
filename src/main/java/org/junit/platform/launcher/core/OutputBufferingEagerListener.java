package org.junit.platform.launcher.core;

import com.example.testoutput.OutputBufferingListener;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;

/**
 * {@link OutputBufferingListener} that handles test completion through JUnit's internal
 * {@link CompositeTestExecutionListener.EagerTestExecutionListener} callback, which runs before any regular
 * listener's {@code executionFinished}. That way a failed test's output is written while Surefire still attributes
 * it to that test.
 * <p>
 * Lives in JUnit's package because the interface is package-private; it exists unchanged from JUnit Platform 1.8 up
 * to at least 6.0. Loaded reflectively by {@code OutputBufferingSessionListener}, which falls back to the plain
 * listener when this class cannot be loaded.
 */
public class OutputBufferingEagerListener extends OutputBufferingListener
        implements CompositeTestExecutionListener.EagerTestExecutionListener {

    @Override
    public void executionJustFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        super.executionFinished(testIdentifier, testExecutionResult);
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        // already handled in executionJustFinished
    }
}
