package com.example.it;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(properties = "context=failing")
class FailingBootTest {

    private static final Logger log = LoggerFactory.getLogger(FailingBootTest.class);

    @Test
    void failing() {
        log.info("BOOT-FAIL-LOG");
        System.out.println("BOOT-FAIL-OUT");
        fail("boom");
    }
}
