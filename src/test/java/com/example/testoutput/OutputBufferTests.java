package com.example.testoutput;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutputBufferTests {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @Test
    void drainsInOrderToTheRightStream() {
        OutputBuffer buffer = new OutputBuffer(1024);
        append(buffer, Target.OUT, "one\n");
        append(buffer, Target.ERR, "two\n");
        append(buffer, Target.OUT, "three\n");

        drain(buffer);

        assertEquals("one\nthree\n", out.toString(StandardCharsets.UTF_8));
        assertEquals("two\n", err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void dropsOldestEntriesWhenFullAndReportsTruncation() {
        OutputBuffer buffer = new OutputBuffer(10);
        append(buffer, Target.OUT, "aaaa\n");
        append(buffer, Target.OUT, "bbbb\n");
        append(buffer, Target.OUT, "cccc\n");

        drain(buffer);

        assertEquals("bbbb\ncccc\n", out.toString(StandardCharsets.UTF_8));
        assertEquals("[test-output-buffer] 5 bytes truncated" + System.lineSeparator(),
                err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void drainEmptiesTheBuffer() {
        OutputBuffer buffer = new OutputBuffer(1024);
        append(buffer, Target.OUT, "one\n");

        drain(buffer);
        out.reset();
        drain(buffer);

        assertEquals("", out.toString(StandardCharsets.UTF_8));
    }

    private static void append(OutputBuffer buffer, Target target, String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        buffer.append(target, bytes, 0, bytes.length);
    }

    private void drain(OutputBuffer buffer) {
        buffer.drainTo(new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
    }
}
