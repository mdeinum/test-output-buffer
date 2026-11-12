package com.example.it;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "context=passing")
class PassingBootTest {

    private static final Logger log = LoggerFactory.getLogger(PassingBootTest.class);

    @Test
    void passing() {
        log.info("BOOT-PASS-LOG");
        System.out.println("BOOT-PASS-OUT");
    }
}
