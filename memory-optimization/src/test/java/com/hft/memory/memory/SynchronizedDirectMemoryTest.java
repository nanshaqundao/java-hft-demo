package com.hft.memory.memory;

import org.junit.jupiter.api.DisplayName;

/**
 * SynchronizedDirectMemory 策略专用测试
 */
@DisplayName("SynchronizedDirectMemory Tests")
public class SynchronizedDirectMemoryTest extends DirectMemoryStrategyTestBase {
    
    @Override
    protected DirectMemoryStrategy createStrategy(int bufferSize) {
        return new SynchronizedDirectMemory(bufferSize);
    }
    
    @Override
    protected String getStrategyName() {
        return "SynchronizedDirectMemory";
    }
}