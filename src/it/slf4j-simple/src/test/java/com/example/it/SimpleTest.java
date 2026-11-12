package com.example.it;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.fail;

class SimpleTest {

    private static final Logger log = LoggerFactory.getLogger(SimpleTest.class);

    @Test
    void passing() {
        log.info("S-PASS-LOG");
        System.out.println("S-PASS-OUT");
    }

    @Test
    void failing() {
        log.info("S-FAIL-LOG");
        System.out.println("S-FAIL-OUT");
        System.err.println("S-FAIL-ERR");
        fail("boom");
    }
}
