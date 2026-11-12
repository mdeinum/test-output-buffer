package com.example.testoutput;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Replacement for the bytes behind {@code System.out}/{@code System.err}.
 * <p>
 * Output from a logging framework is already formatted and goes straight into the current test's buffer. Lines
 * written directly to the console are routed through SLF4J (logger named after the calling class) so they end up
 * in the regular log format; the logging framework then writes them back to the console, where they are buffered
 * like any other log output.
 * <p>
 * Routing must not happen while the {@link java.io.PrintStream} lock is held: logging frameworks take their own
 * lock first and then write to the console, so logging from inside {@code write} would deadlock. Completed lines
 * are therefore queued per thread and routed by {@link #routeQueuedLines()}, which {@link InterceptingPrintStream}
 * calls once the print call returned.
 */
final class InterceptingOutputStream extends OutputStream {

    /** Set while a captured line is being logged, so the resulting log output is buffered as-is. */
    private static final ThreadLocal<Boolean> ROUTING = new ThreadLocal<>();

    private record Line(String caller, byte[] bytes) {
    }

    private static final class PerThread {
        final ByteArrayOutputStream partial = new ByteArrayOutputStream();
        String caller = OutputClassifier.UNKNOWN_CALLER;
        final Deque<Line> queued = new ArrayDeque<>();
    }

    private final Target target;
    private final TestOutputRegistry registry;
    private final OutputClassifier classifier;
    private final Charset charset;
    private final String level;
    private final boolean routeToSlf4j;
    private final ThreadLocal<PerThread> perThread = ThreadLocal.withInitial(PerThread::new);

    InterceptingOutputStream(Target target, TestOutputRegistry registry, OutputClassifier classifier,
            Charset charset, String level, boolean routeToSlf4j) {
        this.target = target;
        this.registry = registry;
        this.classifier = classifier;
        this.charset = charset;
        this.level = level;
        this.routeToSlf4j = routeToSlf4j;
    }

    @Override
    public void write(int b) {
        write(new byte[] { (byte) b }, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (ROUTING.get() != null) {
            registry.append(target, b, off, len);
            return;
        }
        String caller = classifier.directCaller();
        if (caller == null) {
            registry.append(target, b, off, len);
            return;
        }
        PerThread state = perThread.get();
        state.caller = caller;
        int start = off;
        int end = off + len;
        for (int i = off; i < end; i++) {
            if (b[i] == '\n') {
                state.partial.write(b, start, i + 1 - start);
                queue(state);
                start = i + 1;
            }
        }
        if (start < end) {
            state.partial.write(b, start, end - start);
        }
    }

    /**
     * Queues a trailing line without line separator written by the current thread, e.g. before its test finishes,
     * and routes everything queued.
     */
    void completePendingLine() {
        PerThread state = perThread.get();
        if (state.partial.size() > 0) {
            state.partial.write('\n');
            queue(state);
        }
        routeQueuedLines();
    }

    /**
     * Routes the lines completed by the current thread. Must be called without holding the print stream's lock.
     */
    void routeQueuedLines() {
        if (ROUTING.get() != null) {
            return;
        }
        Deque<Line> queued = perThread.get().queued;
        Line line;
        while ((line = queued.pollFirst()) != null) {
            if (!(routeToSlf4j && route(line))) {
                registry.append(target, line.bytes(), 0, line.bytes().length);
            }
        }
    }

    private void queue(PerThread state) {
        state.queued.addLast(new Line(state.caller, state.partial.toByteArray()));
        state.partial.reset();
    }

    private boolean route(Line line) {
        byte[] bytes = line.bytes();
        int length = bytes.length;
        while (length > 0 && (bytes[length - 1] == '\n' || bytes[length - 1] == '\r')) {
            length--;
        }
        String text = new String(bytes, 0, length, charset);
        ROUTING.set(Boolean.TRUE);
        try {
            return Slf4jRouter.log(line.caller(), level, text);
        }
        catch (RuntimeException | LinkageError ex) {
            return false;
        }
        finally {
            ROUTING.remove();
        }
    }
}
