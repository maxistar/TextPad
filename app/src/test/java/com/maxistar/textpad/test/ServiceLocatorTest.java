package com.maxistar.textpad.test;

import com.maxistar.textpad.ServiceLocator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ServiceLocatorTest {
    @Test
    public void test() {
        ServiceLocator locator = ServiceLocator.getInstance();
        assertNotNull(locator);
    }
}
