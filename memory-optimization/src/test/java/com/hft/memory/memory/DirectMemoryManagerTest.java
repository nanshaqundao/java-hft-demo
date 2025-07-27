package com.hft.memory.memory;

import com.hft.memory.core.Order;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DirectMemoryManagerTest {
    
    private DirectMemoryManager manager;
    private static final int BUFFER_SIZE = 1024; // 16 orders capacity
    
    @BeforeEach
    void setUp() {
        manager = new DirectMemoryManager(BUFFER_SIZE);
    }
    
    @Test
    void testBasicSerializationAndDeserialization() {
        // 创建测试订单
        Order original = new Order(123, "EURUSD", 1.2345, 1000);
        
        // 序列化
        boolean result = manager.serializeOrder(original);
        assertTrue(result, "序列化应该成功");
        
        // 反序列化
        Order reusableOrder = new Order();
        Order deserialized = manager.deserializeOrder(reusableOrder, 0);
        
        assertNotNull(deserialized, "反序列化应该成功");
        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getSymbol(), deserialized.getSymbol());
        assertEquals(original.getPrice(), deserialized.getPrice(), 0.0001);
        assertEquals(original.getQuantity(), deserialized.getQuantity());
    }
    
    @Test
    void testMultipleOrdersSerialization() {
        Order order1 = new Order(1, "EURUSD", 1.1234, 1000);
        Order order2 = new Order(2, "GBPUSD", 1.3456, 2000);
        Order order3 = new Order(3, "USDJPY", 110.55, 500);
        
        // 序列化三个订单
        assertTrue(manager.serializeOrder(order1));
        assertTrue(manager.serializeOrder(order2));
        assertTrue(manager.serializeOrder(order3));
        
        // 验证内存使用
        assertEquals(192, manager.getUsedSize()); // 3 * 64 bytes
        assertEquals(13, manager.getRemainingOrderCapacity()); // (1024-192)/64
        
        // 反序列化验证
        Order reusable = new Order();
        
        Order result1 = manager.deserializeOrder(reusable, 0);
        assertEquals(1, result1.getId());
        assertEquals("EURUSD", result1.getSymbol());
        
        Order result2 = manager.deserializeOrder(reusable, 64);
        assertEquals(2, result2.getId());
        assertEquals("GBPUSD", result2.getSymbol());
        
        Order result3 = manager.deserializeOrder(reusable, 128);
        assertEquals(3, result3.getId());
        assertEquals("USDJPY", result3.getSymbol());
    }
    
    @Test
    void testBufferOverflow() {
        // 填满缓冲区
        int maxOrders = BUFFER_SIZE / 64; // 16 orders
        
        for (int i = 0; i < maxOrders; i++) {
            Order order = new Order(i, "TEST" + i, 1.0 + i, 1000 + i);
            assertTrue(manager.serializeOrder(order), "订单 " + i + " 应该成功序列化");
        }
        
        // 尝试写入第17个订单，应该失败
        Order overflowOrder = new Order(999, "OVERFLOW", 999.0, 999);
        assertFalse(manager.serializeOrder(overflowOrder), "缓冲区满时应该返回false");
    }
    
    @Test
    void testRingBufferBehavior() {
        // 填满缓冲区
        int maxOrders = BUFFER_SIZE / 64; // 16 orders
        
        for (int i = 0; i < maxOrders; i++) {
            Order order = new Order(i, "TEST" + i, 1.0 + i, 1000 + i);
            manager.serializeOrder(order);
        }
        
        // 使用环形缓冲区模式，应该从头开始覆盖
        Order newOrder = new Order(999, "RING", 999.0, 999);
        assertTrue(manager.serializeOrderRing(newOrder), "环形缓冲区应该成功覆盖");
        
        // 验证第一个位置被覆盖
        Order reusable = new Order();
        Order result = manager.deserializeOrder(reusable, 0);
        assertEquals(999, result.getId());
        assertEquals("RING", result.getSymbol());
    }
    
    @Test
    void testThreadSafety() throws InterruptedException {
        final int threadCount = 10;
        final int ordersPerThread = 5;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < ordersPerThread; i++) {
                        Order order = new Order(threadId * 1000 + i, "THREAD" + threadId, 
                                              1.0 + threadId, 1000 + i);
                        if (manager.serializeOrder(order)) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        
        int total = successCount.get() + failureCount.get();
        assertEquals(threadCount * ordersPerThread, total, "所有操作都应该完成");
        
        // 验证没有数据竞争导致的内存使用错误
        assertTrue(manager.getUsedSize() >= 0, "内存使用不应该为负数");
        assertTrue(manager.getUsedSize() <= BUFFER_SIZE, "内存使用不应该超过缓冲区大小");
    }
    
    @Test
    void testDeserializationThreadSafety() throws InterruptedException {
        // 先写入一些订单
        for (int i = 0; i < 5; i++) {
            Order order = new Order(i, "TEST" + i, 1.0 + i, 1000 + i);
            manager.serializeOrder(order);
        }
        
        final int readerThreads = 5;
        final CountDownLatch latch = new CountDownLatch(readerThreads);
        final AtomicInteger readCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(readerThreads);
        
        for (int t = 0; t < readerThreads; t++) {
            executor.submit(() -> {
                try {
                    // 使用线程安全的反序列化方法
                    for (int i = 0; i < 5; i++) {
                        Order result = manager.deserializeOrderSafe(i * 64);
                        if (result != null && result.getId() == i) {
                            readCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertEquals(readerThreads * 5, readCount.get(), "所有读操作都应该成功");
    }
    
    @Test
    void testReset() {
        // 写入一些数据
        for (int i = 0; i < 3; i++) {
            Order order = new Order(i, "TEST" + i, 1.0 + i, 1000);
            manager.serializeOrder(order);
        }
        
        assertTrue(manager.getUsedSize() > 0, "重置前应该有数据");
        
        // 重置
        manager.reset();
        
        assertEquals(0, manager.getUsedSize(), "重置后使用大小应该为0");
        assertEquals(16, manager.getRemainingOrderCapacity(), "重置后应该恢复全部容量");
    }
    
    @Test
    void testForceReset() {
        // 写入一些数据
        for (int i = 0; i < 3; i++) {
            Order order = new Order(i, "TEST" + i, 1.0 + i, 1000);
            manager.serializeOrder(order);
        }
        
        assertTrue(manager.getUsedSize() > 0, "强制重置前应该有数据");
        
        // 强制重置
        manager.forceReset();
        
        assertEquals(0, manager.getUsedSize(), "强制重置后使用大小应该为0");
    }
    
    @Test
    void testUtilityMethods() {
        assertEquals(BUFFER_SIZE, manager.getBufferSize());
        assertEquals(0, manager.getUsedSize());
        assertEquals(BUFFER_SIZE, manager.getRemainingSize());
        assertEquals(16, manager.getRemainingOrderCapacity());
        assertFalse(manager.isAlmostFull(0.8));
        
        // 写入一些数据后再测试
        for (int i = 0; i < 13; i++) {  // 13/16 = 81.25%
            Order order = new Order(i, "TEST" + i, 1.0, 1000);
            manager.serializeOrder(order);
        }
        
        assertTrue(manager.isAlmostFull(0.8), "超过80%应该返回true");
        assertEquals(3, manager.getRemainingOrderCapacity(), "应该剩余3个订单容量");
    }
    
    @Test
    void testNullSymbolHandling() {
        Order orderWithNullSymbol = new Order();
        orderWithNullSymbol.setId(123);
        orderWithNullSymbol.setSymbol(null);  // null symbol
        orderWithNullSymbol.setPrice(1.234);
        orderWithNullSymbol.setQuantity(1000);
        
        assertTrue(manager.serializeOrder(orderWithNullSymbol), "应该能处理null symbol");
        
        Order reusable = new Order();
        Order result = manager.deserializeOrder(reusable, 0);
        
        assertEquals(123, result.getId());
        assertEquals("", result.getSymbol());  // null应该变成空字符串
    }
}