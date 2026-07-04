package com.maxistar.textpad.syntax;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class SyntaxPerformanceFixtureTest {
    @Test
    void highTokenCountEligibleDocumentCompletesWithinFixtureBudget() {
        StringBuilder source = new StringBuilder("[");
        for (int index = 0; index < 5_000; index++) {
            source.append("{\"value\":").append(index).append("},");
        }
        source.append("{}]");

        assertTimeout(Duration.ofSeconds(2), () -> {
            SyntaxTokenizationResult result =
                    new JsonSyntaxTokenizer().tokenize(source.toString(), 40_000);
            assertFalse(result.isTokenLimitExceeded());
            assertTrue(result.getTokens().size() > 20_000);
        });
    }

    @Test
    void repeatedGenerationInvalidationKeepsOnlyNewestGeneration() {
        SyntaxGeneration generations = new SyntaxGeneration();
        long newest = 0;
        for (int index = 0; index < 10_000; index++) {
            newest = generations.next();
        }
        assertTrue(generations.isCurrent(newest));
    }
}
