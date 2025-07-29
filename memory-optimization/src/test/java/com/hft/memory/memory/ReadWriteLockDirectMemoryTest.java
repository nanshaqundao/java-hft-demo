package com.hft.memory.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.hft.memory.core.Order;

/**
 * ReadWriteLockDirectMemory 策略专用测试
 * 包含读写锁特有的测试用例
 */
@DisplayName("ReadWriteLockDirectMemory Tests")
public class ReadWriteLockDirectMemoryTest extends DirectMemoryStrategyTestBase {
    
    @Override
    protected DirectMemoryStrategy createStrategy(int bufferSize) {
        return new ReadWriteLockDirectMemory(bufferSize);
    }
    
    @Override
    protected String getStrategyName() {
        return "ReadWriteLockDirectMemory";
    }
    
    @Test
    void testConcurrentReadsPerformance() throws InterruptedException {
        System.out.println("  📖 测试读写锁并发读取性能...");
        
        // 预先写入数据
        for (int i = 0; i < 8; i++) {
            Order order = new Order();
            order.setId(i);
            order.setSymbol("READ" + i);
            order.setPriceAndQuantity(1.0 + i * 0.1, 1000 + i);
            order.setSide((byte) 1);
            order.setTimestamp(System.nanoTime());
            strategy.serializeOrder(order);
        }
        
        // 大量并发读取 - 读写锁应该允许并发读取
        final int readerThreads = 20; // 更多读取线程
        final int readsPerThread = 50;
        
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(readerThreads);
        java.util.concurrent.atomic.AtomicInteger totalReads = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicLong totalTime = new java.util.concurrent.atomic.AtomicLong(0);
        
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(readerThreads);
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < readerThreads; t++) {
            executor.submit(() -> {
                try {
                    long threadStart = System.nanoTime();
                    
                    for (int i = 0; i < readsPerThread; i++) {
                        int offset = (i % 8) * 64; // 读取不同位置
                        Order result = strategy.deserializeOrderSafe(offset);
                        
                        if (result != null) {
                            totalReads.incrementAndGet();
                        }
                    }
                    
                    long threadTime = System.nanoTime() - threadStart;
                    totalTime.addAndGet(threadTime);
                    
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(15, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.nanoTime();
        double totalTimeMs = (endTime - startTime) / 1_000_000.0;
        double readsPerSecond = totalReads.get() * 1000.0 / totalTimeMs;
        
        System.out.printf("    总读取: %d, 耗时: %.2f ms, 读取/秒: %.0f%n", 
                         totalReads.get(), totalTimeMs, readsPerSecond);
        
        assertEquals(readerThreads * readsPerThread, totalReads.get(), 
                    "所有读取都应该成功");
    }
    
    @Test
    void testReadWhileWrite() throws InterruptedException {
        System.out.println("  🔄 测试读写并发...");
        
        // 预先写入一些数据
        for (int i = 0; i < 3; i++) {
            Order order = new Order();
            order.setId(i);
            order.setSymbol("RW" + i);
            order.setPriceAndQuantity(1.0 + i, 1000);
            order.setSide((byte) 1);
            order.setTimestamp(System.nanoTime());
            strategy.serializeOrder(order);
        }
        
        final int duration = 2; // 2秒测试
        java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean(true);
        java.util.concurrent.atomic.AtomicInteger reads = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger writes = new java.util.concurrent.atomic.AtomicInteger(0);
        
        // 多个读取线程
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newCachedThreadPool();
        
        // 启动读取线程
        for (int i = 0; i < 6; i++) {
            executor.submit(() -> {
                while (running.get()) {
                    Order result = strategy.deserializeOrderSafe(0);
                    if (result != null) {
                        reads.incrementAndGet();
                    }
                    try {
                        Thread.sleep(1); // 1ms间隔
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        
        // 启动写入线程
        for (int i = 0; i < 2; i++) {
            final int writerId = i;
            executor.submit(() -> {
                int counter = 0;
                while (running.get()) {
                    Order order = new Order();
                    order.setId(1000 + writerId * 100 + counter);
                    order.setSymbol("W" + writerId);
                    order.setPriceAndQuantity(2.0 + writerId, 2000);
                    order.setSide((byte) 1);
                    order.setTimestamp(System.nanoTime());
                    
                    if (strategy.serializeOrder(order)) {
                        writes.incrementAndGet();
                    }
                    
                    counter++;
                    try {
                        Thread.sleep(10); // 10ms间隔，写入频率较低
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        
        // 运行指定时间
        Thread.sleep(duration * 1000);
        running.set(false);
        
        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        
        System.out.printf("    %d秒内 - 读取: %d, 写入: %d%n", 
                         duration, reads.get(), writes.get());
        
        assertTrue(reads.get() > 0, "应该有成功的读取");
        assertTrue(writes.get() > 0, "应该有成功的写入");
        
        // 读写锁应该允许更多的读取操作
        assertTrue(reads.get() > writes.get() * 10, 
                  "读取次数应该远多于写入次数（读写锁优势）");
    }
    
    @Test
    void testBatchWriteOptimization() {
        System.out.println("  📦 测试批量写入优化...");
        
        // 创建批量订单
        java.util.List<Order> orders = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Order order = new Order();
            order.setId(100 + i);
            order.setSymbol("BATCH" + i);
            order.setPriceAndQuantity(1.5 + i * 0.1, 1500 + i);
            order.setSide((byte) 1);
            order.setTimestamp(System.nanoTime());
            orders.add(order);
        }
        
        // 测试批量写入
        long startTime = System.nanoTime();
        int batchResult = strategy.serializeOrderBatch(orders);
        long batchTime = System.nanoTime() - startTime;
        
        assertEquals(5, batchResult, "批量写入应该全部成功");
        
        // 验证批量写入的数据
        for (int i = 0; i < 5; i++) {
            Order result = strategy.deserializeOrderSafe(i * 64);
            assertNotNull(result, "反序列化应该成功");
            assertEquals(100 + i, result.getId(), "ID应该匹配");
            assertEquals("BATCH" + i, result.getSymbol(), "Symbol应该匹配");
        }
        
        System.out.printf("    批量写入5个订单耗时: %.2f μs%n", batchTime / 1000.0);
    }
}