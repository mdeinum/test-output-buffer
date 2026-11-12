package com.example.it;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.fail;

class ParallelBTest {

    private static final Logger log = LoggerFactory.getLogger(ParallelBTest.class);

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })
    void run(int i) throws Exception {
        boolean failing = i % 5 == 0;
        String marker = (failing ? "FAIL-" : "PASS-") + "B" + i;
        for (int line = 0; line < 5; line++) {
            log.info("{}-LOG-{}", marker, line);
            System.out.println(marker + "-OUT-" + line);
            Thread.sleep(2);
        }
        if (failing) {
            fail("boom " + marker);
        }
    }
}
