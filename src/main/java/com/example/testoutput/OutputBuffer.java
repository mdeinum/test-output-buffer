package com.example.testoutput;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Ordered, size-capped buffer of console-ready output for a single test or container.
 */
final class OutputBuffer {

    private record Entry(Target target, byte[] bytes) {
    }

    private final int maxBytes;
    private final Deque<Entry> entries = new ArrayDeque<>();
    private long size;
    private long truncatedBytes;

    OutputBuffer(int maxBytes) {
        this.maxBytes = maxBytes;
    }

    synchronized void append(Target target, byte[] bytes, int off, int len) {
        if (len == 0) {
            return;
        }
        byte[] copy = new byte[len];
        System.arraycopy(bytes, off, copy, 0, len);
        entries.addLast(new Entry(target, copy));
        size += len;
        while (size > maxBytes && entries.size() > 1) {
            Entry dropped = entries.removeFirst();
            size -= dropped.bytes().length;
            truncatedBytes += dropped.bytes().length;
        }
    }

    /**
     * Writes all buffered output to the given streams and empties the buffer.
     */
    synchronized void drainTo(PrintStream out, PrintStream err) {
        if (truncatedBytes > 0) {
            byte[] marker = ("[test-output-buffer] " + truncatedBytes + " bytes truncated"
                    + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
            err.write(marker, 0, marker.length);
        }
        for (Entry entry : entries) {
            PrintStream stream = entry.target() == Target.OUT ? out : err;
            stream.write(entry.bytes(), 0, entry.bytes().length);
        }
        out.flush();
        err.flush();
        discard();
    }

    synchronized void discard() {
        entries.clear();
        size = 0;
        truncatedBytes = 0;
    }
}
