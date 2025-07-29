package com.hft.memory.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.hft.memory.core.Order;

/**
 * CASDirectMemory 策略专用测试
 * 包含CAS特有的测试用例
 */
@DisplayName("CASDirectMemory Tests")
public class CASDirectMemoryTest extends DirectMemoryStrategyTestBase {
    
    @Override
    protected DirectMemoryStrategy createStrategy(int bufferSize) {
        return new CASDirectMemory(bufferSize);
    }
    
    @Override
    protected String getStrategyName() {
        return "CASDirectMemory";
    }
    
    @Test
    void testHighConcurrencyWrites() throws InterruptedException {
        System.out.println("  ⚡ 测试CAS高并发写入...");
        
        // CAS策略应该在高并发下表现更好
        final int threadCount = 16; // 更多线程
        final int ordersPerThread = 1;
        
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger retryCount = new java.util.concurrent.atomic.AtomicInteger(0);
        
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < ordersPerThread; i++) {
                        Order order = new Order();
                        order.setId(threadId * 1000 + i);
                        order.setSymbol("CAS" + threadId);
                        order.setPriceAndQuantity(1.0 + threadId, 1000 + i);
                        order.setSide((byte) 1);
                        order.setTimestamp(System.nanoTime());
                        
                        // CAS可能需要重试
                        int attempts = 0;
                        while (attempts < 10) {
                            if (strategy.serializeOrder(order)) {
                                successCount.incrementAndGet();
                                break;
                            }
                            retryCount.incrementAndGet();
                            attempts++;
                            
                            // 短暂等待避免活锁
                            if (attempts > 5) {
                                try {
                                    Thread.sleep(0, 1000); // 1微秒
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();
        
        System.out.printf("    CAS成功写入: %d, 重试次数: %d%n", 
                         successCount.get(), retryCount.get());
        
        // CAS策略应该有一些成功的写入
        assertTrue(successCount.get() > 0, "CAS策略应该有成功的写入");
    }
    
    @Test
    void testVersionConsistency() {
        System.out.println("  🔄 测试CAS版本一致性...");
        
        // 写入一个订单
        Order original = new Order();
        original.setId(42);
        original.setSymbol("VERSION");
        original.setPriceAndQuantity(1.42, 4200);
        original.setSide((byte) 1);
        original.setTimestamp(System.nanoTime());
        
        assertTrue(strategy.serializeOrder(original), "写入应该成功");
        
        // 多次读取，验证版本一致性
        for (int i = 0; i < 10; i++) {
            Order read = strategy.deserializeOrderSafe(0);
            assertNotNull(read, "读取应该成功");
            assertEquals(42, read.getId(), "ID应该一致");
            assertEquals("VERSION", read.getSymbol(), "Symbol应该一致");
            assertEquals(1.42, read.getPrice(), 0.0001, "Price应该一致");
            assertEquals(4200, read.getQuantity(), "Quantity应该一致");
        }
    }
}