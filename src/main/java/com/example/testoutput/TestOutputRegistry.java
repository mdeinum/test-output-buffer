package com.example.testoutput;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps an {@link OutputBuffer} per running test/container and decides which buffer output belongs to.
 * <p>
 * Threads are bound to the test they execute. Output from unbound threads (executors, servers, async appenders)
 * is attributed on a best-effort basis: to the only active test, else to the deepest container shared by all
 * active tests, else to a plan-wide orphan buffer.
 */
final class TestOutputRegistry {

    static final class Node {
        final String id;
        final Node parent;
        final boolean test;
        final OutputBuffer buffer;
        volatile boolean active = true;
        /** Set once a descendant failed: the remaining container output is flushed as well. */
        volatile boolean forceFlush;

        Node(String id, Node parent, boolean test, int maxBytes) {
            this.id = id;
            this.parent = parent;
            this.test = test;
            this.buffer = new OutputBuffer(maxBytes);
        }
    }

    private final BufferConfig config;
    private final PrintStream originalOut;
    private final PrintStream originalErr;
    private final Map<String, Node> nodes = new ConcurrentHashMap<>();
    private final Set<Node> activeTests = ConcurrentHashMap.newKeySet();
    private final InheritableThreadLocal<Node> boundNode = new InheritableThreadLocal<>();
    private final OutputBuffer orphans;
    private final AtomicBoolean anyFailure = new AtomicBoolean();
    private final Object flushLock = new Object();

    TestOutputRegistry(BufferConfig config, PrintStream originalOut, PrintStream originalErr) {
        this.config = config;
        this.originalOut = originalOut;
        this.originalErr = originalErr;
        this.orphans = new OutputBuffer(config.maxBytesPerTest());
    }

    void started(String id, String parentId, boolean test) {
        Node parent = parentId != null ? nodes.get(parentId) : null;
        Node node = new Node(id, parent, test, config.maxBytesPerTest());
        nodes.put(id, node);
        if (test) {
            activeTests.add(node);
        }
        boundNode.set(node);
    }

    /**
     * Marks the node as finished, flushing its output when {@code failed} (or when a descendant failed).
     */
    void finished(String id, boolean failed) {
        Node node = nodes.remove(id);
        if (node == null) {
            return;
        }
        node.active = false;
        activeTests.remove(node);
        if (failed) {
            anyFailure.set(true);
            synchronized (flushLock) {
                flushAncestors(node);
                node.buffer.drainTo(originalOut, originalErr);
            }
        }
        else if (node.forceFlush) {
            synchronized (flushLock) {
                node.buffer.drainTo(originalOut, originalErr);
            }
        }
        else {
            node.buffer.discard();
        }
        Node parent = node.parent;
        if (parent != null && parent.active) {
            boundNode.set(parent);
        }
        else {
            boundNode.remove();
        }
    }

    /**
     * Called when the whole test plan finished: orphaned output is only shown when something failed.
     */
    void planFinished() {
        if (anyFailure.get()) {
            synchronized (flushLock) {
                orphans.drainTo(originalOut, originalErr);
            }
        }
        else {
            orphans.discard();
        }
        nodes.clear();
        activeTests.clear();
        boundNode.remove();
    }

    void append(Target target, byte[] bytes, int off, int len) {
        bufferForCurrentThread().append(target, bytes, off, len);
    }

    private OutputBuffer bufferForCurrentThread() {
        Node node = boundNode.get();
        if (node != null && node.active) {
            return node.buffer;
        }
        Node shared = sharedNode();
        return shared != null ? shared.buffer : orphans;
    }

    private Node sharedNode() {
        List<Node> tests = new ArrayList<>(activeTests);
        if (tests.size() == 1) {
            return tests.get(0);
        }
        if (tests.isEmpty()) {
            return null;
        }
        // deepest container that is an ancestor of every active test
        Node candidate = tests.get(0).parent;
        while (candidate != null) {
            if (candidate.active && isAncestorOfAll(candidate, tests)) {
                return candidate;
            }
            candidate = candidate.parent;
        }
        return null;
    }

    private static boolean isAncestorOfAll(Node candidate, List<Node> tests) {
        for (Node test : tests) {
            Node current = test.parent;
            while (current != null && current != candidate) {
                current = current.parent;
            }
            if (current == null) {
                return false;
            }
        }
        return true;
    }

    private void flushAncestors(Node node) {
        Deque<Node> ancestors = new ArrayDeque<>();
        for (Node current = node.parent; current != null; current = current.parent) {
            ancestors.push(current);
        }
        // root first, so output appears in the order it was produced
        for (Node ancestor : ancestors) {
            ancestor.forceFlush = true;
            ancestor.buffer.drainTo(originalOut, originalErr);
        }
    }
}
