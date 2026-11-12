package com.example.it;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.fail;

public class Junit4Test {

    private static final Logger log = LoggerFactory.getLogger(Junit4Test.class);

    @Test
    public void passing() {
        log.info("J4-PASS-LOG");
        System.out.println("J4-PASS-OUT");
    }

    @Test
    public void failing() {
        log.info("J4-FAIL-LOG");
        System.out.println("J4-FAIL-OUT");
        System.err.println("J4-FAIL-ERR");
        fail("boom");
    }
}
