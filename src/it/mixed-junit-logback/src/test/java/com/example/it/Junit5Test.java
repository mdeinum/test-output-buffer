package com.example.it;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.fail;

class Junit5Test {

    private static final Logger log = LoggerFactory.getLogger(Junit5Test.class);

    @BeforeAll
    static void beforeAll() {
        log.info("J5-BEFORE-ALL");
    }

    @Test
    void passing() {
        log.info("J5-PASS-LOG");
        System.out.println("J5-PASS-OUT");
        System.err.println("J5-PASS-ERR");
    }

    @Test
    void failing() throws Exception {
        log.info("J5-FAIL-LOG");
        System.out.println("J5-FAIL-OUT");
        System.err.println("J5-FAIL-ERR");
        System.out.print("J5-FAIL-PARTIAL");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> log.info("J5-FAIL-BACKGROUND")).get();
        executor.shutdown();
        fail("boom");
    }
}
