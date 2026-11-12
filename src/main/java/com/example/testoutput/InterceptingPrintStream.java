package com.example.testoutput;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * {@link PrintStream} installed as {@code System.out}/{@code System.err}. After each call it routes the lines that
 * were completed, outside of the stream's lock (see {@link InterceptingOutputStream}).
 * <p>
 * {@code PrintStream} subclasses lock on {@code this}; nested calls (e.g. {@code println} calling {@code print})
 * still hold the lock, hence the {@link Thread#holdsLock} check.
 */
final class InterceptingPrintStream extends PrintStream {

    private final InterceptingOutputStream target;

    InterceptingPrintStream(InterceptingOutputStream target, Charset charset) {
        super(target, true, charset);
        this.target = target;
    }

    private void routeCompletedLines() {
        if (!Thread.holdsLock(this)) {
            target.routeQueuedLines();
        }
    }

    @Override
    public void flush() {
        super.flush();
        routeCompletedLines();
    }

    @Override
    public void write(int b) {
        super.write(b);
        routeCompletedLines();
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        super.write(buf, off, len);
        routeCompletedLines();
    }

    @Override
    public void write(byte[] buf) {
        write(buf, 0, buf.length);
    }

    @Override
    public void print(boolean b) {
        super.print(b);
        routeCompletedLines();
    }

    @Override
    public void print(char c) {
        super.print(c);
        routeCompletedLines();
    }

    @Override
    public void print(int i) {
        super.print(i);
        routeCompletedLines();
    }

    @Override
    public void print(long l) {
        super.print(l);
        routeCompletedLines();
    }

    @Override
    public void print(float f) {
        super.print(f);
        routeCompletedLines();
    }

    @Override
    public void print(double d) {
        super.print(d);
        routeCompletedLines();
    }

    @Override
    public void print(char[] s) {
        super.print(s);
        routeCompletedLines();
    }

    @Override
    public void print(String s) {
        super.print(s);
        routeCompletedLines();
    }

    @Override
    public void print(Object obj) {
        super.print(obj);
        routeCompletedLines();
    }

    @Override
    public void println() {
        super.println();
        routeCompletedLines();
    }

    @Override
    public void println(boolean x) {
        super.println(x);
        routeCompletedLines();
    }

    @Override
    public void println(char x) {
        super.println(x);
        routeCompletedLines();
    }

    @Override
    public void println(int x) {
        super.println(x);
        routeCompletedLines();
    }

    @Override
    public void println(long x) {
        super.println(x);
        routeCompletedLines();
    }

    @Override
    public void println(float x) {
        super.println(x);
        routeCompletedLines();
    }

    @Override
    public void println(double x) {
        super.println(x);
        routeCompletedLines();
    }

    @Override
    public void println(char[] x) {
        super.println(x);
        routeCompletedLines();
    }

    @Override
    public void println(String x) {
        super.println(x);
        routeCompletedLines();
    }

    @Override
    public void println(Object x) {
        super.println(x);
        routeCompletedLines();
    }

    @Override
    public PrintStream format(String format, Object... args) {
        super.format(format, args);
        routeCompletedLines();
        return this;
    }

    @Override
    public PrintStream format(Locale l, String format, Object... args) {
        super.format(l, format, args);
        routeCompletedLines();
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq) {
        super.append(csq);
        routeCompletedLines();
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        super.append(csq, start, end);
        routeCompletedLines();
        return this;
    }

    @Override
    public PrintStream append(char c) {
        super.append(c);
        routeCompletedLines();
        return this;
    }
}
