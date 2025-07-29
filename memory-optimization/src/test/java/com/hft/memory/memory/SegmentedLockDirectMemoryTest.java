package com.hft.memory.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.hft.memory.core.Order;

/**
 * SegmentedLockDirectMemory 策略专用测试
 * 包含分段锁特有的测试用例
 */
@DisplayName("SegmentedLockDirectMemory Tests")
public class SegmentedLockDirectMemoryTest extends DirectMemoryStrategyTestBase {
    
    @Override
    protected DirectMemoryStrategy createStrategy(int bufferSize) {
        return new SegmentedLockDirectMemory(bufferSize);
    }
    
    @Override
    protected String getStrategyName() {
        return "SegmentedLockDirectMemory";
    }
    
    @Test
    void testSegmentedConcurrentWrites() throws InterruptedException {
        System.out.println("  🧩 测试分段锁并发写入...");
        
        // 分段锁应该能够支持更好的写入并发
        final int threadCount = 16; // 16个线程，16个段
        final int ordersPerThread = 1;
        
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicLong totalTime = new java.util.concurrent.atomic.AtomicLong(0);
        
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    long threadStart = System.nanoTime();
                    
                    for (int i = 0; i < ordersPerThread; i++) {
                        Order order = new Order();
                        order.setId(threadId * 1000 + i);
                        order.setSymbol("SEG" + threadId);
                        order.setPriceAndQuantity(1.0 + threadId * 0.01, 1000 + i);
                        order.setSide((byte) 1);
                        order.setTimestamp(System.nanoTime());
                        
                        if (strategy.serializeOrder(order)) {
                            successCount.incrementAndGet();
                        }
                    }
                    
                    long threadTime = System.nanoTime() - threadStart;
                    totalTime.addAndGet(threadTime);
                    
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.nanoTime();
        double totalTimeMs = (endTime - startTime) / 1_000_000.0;
        double writesPerSecond = successCount.get() * 1000.0 / totalTimeMs;
        
        System.out.printf("    分段锁写入: %d/%d, 耗时: %.2f ms, 写入/秒: %.0f%n", 
                         successCount.get(), threadCount * ordersPerThread, 
                         totalTimeMs, writesPerSecond);
        
        // 分段锁应该有很好的并发性能
        assertTrue(successCount.get() >= threadCount * ordersPerThread / 2, 
                  "分段锁应该支持良好的并发写入");
    }
    
    @Test
    void testSegmentDistribution() {
        System.out.println("  📊 测试段分布...");
        
        // 写入多个订单，观察它们在不同段中的分布
        final int orderCount = 32; // 超过段数量
        int successCount = 0;
        
        for (int i = 0; i < orderCount; i++) {
            Order order = new Order();
            order.setId(i);
            order.setSymbol("DIST" + i);
            order.setPriceAndQuantity(1.0 + i * 0.01, 1000 + i);
            order.setSide((byte) 1);
            order.setTimestamp(System.nanoTime());
            
            if (strategy.serializeOrder(order)) {
                successCount++;
            }
        }
        
        System.out.printf("    成功写入: %d/%d 订单%n", successCount, orderCount);
        
        // 验证能够写入的订单数量
        assertTrue(successCount > 0, "应该能够写入一些订单");
        
        // 验证内存使用情况
        int usedSize = strategy.getUsedSize();
        assertTrue(usedSize > 0, "应该使用了一些内存");
        assertEquals(successCount * 64, usedSize, "内存使用应该等于订单数*64字节");
    }
    
    @Test
    void testSegmentedRingBuffer() throws InterruptedException {
        System.out.println("  🔄 测试分段环形缓冲区...");
        
        // 填满缓冲区
        int maxOrders = BUFFER_SIZE / 64; // 16 orders
        
        for (int i = 0; i < maxOrders; i++) {
            Order order = new Order();
            order.setId(i);
            order.setSymbol("RING" + i);
            order.setPriceAndQuantity(1.0 + i * 0.01, 1000);
            order.setSide((byte) 1);
            order.setTimestamp(System.nanoTime());
            strategy.serializeOrder(order);
        }
        
        // 验证缓冲区已满
        Order overflow = new Order();
        overflow.setId(999);
        overflow.setSymbol("OVERFLOW");
        overflow.setPriceAndQuantity(9.99, 9999);
        overflow.setSide((byte) 1);
        overflow.setTimestamp(System.nanoTime());
        
        assertFalse(strategy.serializeOrder(overflow), "缓冲区满时应该返回false");
        
        // 使用环形缓冲区模式，应该能够覆盖写入
        Order ringOrder = new Order();
        ringOrder.setId(888);
        ringOrder.setSymbol("RINGTEST");
        ringOrder.setPriceAndQuantity(8.88, 8888);
        ringOrder.setSide((byte) 1);
        ringOrder.setTimestamp(System.nanoTime());
        
        assertTrue(strategy.serializeOrderRing(ringOrder), 
                  "分段环形缓冲区应该支持覆盖写入");
    }
    
    @Test
    void testConcurrentSegmentAccess() throws InterruptedException {
        System.out.println("  🔀 测试并发段访问...");
        
        // 多线程同时访问不同的段
        final int threadCount = 8;
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicInteger totalOperations = new java.util.concurrent.atomic.AtomicInteger(0);
        
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    // 每个线程写入和读取
                    for (int i = 0; i < 3; i++) {
                        // 写入
                        Order order = new Order();
                        order.setId(threadId * 100 + i);
                        order.setSymbol("T" + threadId + "O" + i);
                        order.setPriceAndQuantity(1.0 + threadId + i * 0.1, 1000 + i);
                        order.setSide((byte) 1);
                        order.setTimestamp(System.nanoTime());
                        
                        if (strategy.serializeOrder(order)) {
                            totalOperations.incrementAndGet();
                            
                            // 立即尝试读取（可能读取到不同段的数据）
                            Order result = strategy.deserializeOrderSafe(0);
                            if (result != null) {
                                totalOperations.incrementAndGet();
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
        
        System.out.printf("    总操作数: %d%n", totalOperations.get());
        assertTrue(totalOperations.get() > 0, "应该有成功的操作");
    }
    
    @Test
    void testSegmentLoadBalancing() {
        System.out.println("  ⚖️ 测试段负载均衡...");
        
        // 快速连续写入，观察分段锁的负载均衡效果
        final int orderCount = 20;
        int successCount = 0;
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < orderCount; i++) {
            Order order = new Order();
            order.setId(i);
            order.setSymbol("LB" + i);
            order.setPriceAndQuantity(1.0 + i * 0.01, 1000);
            order.setSide((byte) 1);
            order.setTimestamp(System.nanoTime());
            
            if (strategy.serializeOrder(order)) {
                successCount++;
            }
        }
        
        long endTime = System.nanoTime();
        double timeMs = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("    连续写入: %d/%d, 耗时: %.2f ms%n", 
                         successCount, orderCount, timeMs);
        
        assertTrue(successCount > 0, "应该能够写入订单");
        
        // 验证快速写入的效率
        if (successCount > 0) {
            double avgTimePerOrder = timeMs / successCount;
            System.out.printf("    平均每订单: %.2f ms%n", avgTimePerOrder);
            assertTrue(avgTimePerOrder < 1.0, "分段锁应该提供较快的写入速度");
        }
    }
}