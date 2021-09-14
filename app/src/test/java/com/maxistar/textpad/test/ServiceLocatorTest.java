package com.maxistar.textpad.test;

import com.maxistar.textpad.ServiceLocator;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ServiceLocatorTest {
    @Test
    public void test() {
        ServiceLocator locator = ServiceLocator.getInstance();
        assertNotNull(locator);
    }
}
