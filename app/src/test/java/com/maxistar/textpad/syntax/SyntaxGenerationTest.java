package com.maxistar.textpad.syntax;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SyntaxGenerationTest {
    @Test
    void onlyLatestGenerationIsCurrent() {
        SyntaxGeneration generations = new SyntaxGeneration();
        long first = generations.next();
        long second = generations.next();

        assertFalse(generations.isCurrent(first));
        assertTrue(generations.isCurrent(second));
    }
}
