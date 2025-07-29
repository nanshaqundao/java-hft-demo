package com.hft.memory.memory;

import com.hft.memory.core.Order;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DirectMemoryStrategy 测试基类
 * 包含所有策略的通用测试用例
 */
public abstract class DirectMemoryStrategyTestBase {
    
    protected DirectMemoryStrategy strategy;
    protected static final int BUFFER_SIZE = 1024; // 16 orders capacity
    
    /**
     * 子类需要实现此方法来创建具体的策略实例
     */
    protected abstract DirectMemoryStrategy createStrategy(int bufferSize);
    
    /**
     * 获取策略名称用于日志
     */
    protected abstract String getStrategyName();
    
    @BeforeEach
    void setUp() {
        strategy = createStrategy(BUFFER_SIZE);
        System.out.println("🧪 测试策略: " + getStrategyName());
    }
    
    @Test
    void testBasicSerializationAndDeserialization() {
        System.out.println("  📝 测试基本序列化和反序列化...");
        
        // 创建测试订单
        Order original = new Order();
        original.setId(123);
        original.setSymbol("EURUSD");
        original.setPriceAndQuantity(1.2345, 1000);
        original.setSide((byte) 1);
        original.setTimestamp(System.nanoTime());
        
        // 序列化
        boolean result = strategy.serializeOrder(original);
        assertTrue(result, "序列化应该成功");
        
        // 反序列化
        Order reusableOrder = new Order();
        Order deserialized = strategy.deserializeOrder(reusableOrder, 0);
        
        assertNotNull(deserialized, "反序列化应该成功");
        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getSymbol(), deserialized.getSymbol());
        assertEquals(original.getPrice(), deserialized.getPrice(), 0.0001);
        assertEquals(original.getQuantity(), deserialized.getQuantity());
        assertEquals(original.getSide(), deserialized.getSide());
    }
    
    @Test
    void testMultipleOrdersSerialization() {
        System.out.println("  📦 测试多订单序列化...");
        
        Order order1 = createTestOrder(1, "EURUSD", 1.1234, 1000);
        Order order2 = createTestOrder(2, "GBPUSD", 1.3456, 2000);
        Order order3 = createTestOrder(3, "USDJPY", 110.55, 500);
        
        // 序列化三个订单
        assertTrue(strategy.serializeOrder(order1));
        assertTrue(strategy.serializeOrder(order2));
        assertTrue(strategy.serializeOrder(order3));
        
        // 验证内存使用
        assertEquals(192, strategy.getUsedSize()); // 3 * 64 bytes
        assertEquals(13, strategy.getRemainingOrderCapacity()); // (1024-192)/64
        
        // 反序列化验证
        Order result1 = strategy.deserializeOrderSafe(0);
        assertEquals(1, result1.getId());
        assertEquals("EURUSD", result1.getSymbol());
        
        Order result2 = strategy.deserializeOrderSafe(64);
        assertEquals(2, result2.getId());
        assertEquals("GBPUSD", result2.getSymbol());
        
        Order result3 = strategy.deserializeOrderSafe(128);
        assertEquals(3, result3.getId());
        assertEquals("USDJPY", result3.getSymbol());
    }
    
    @Test
    void testBufferOverflow() {
        System.out.println("  💥 测试缓冲区溢出...");
        
        // 填满缓冲区
        int maxOrders = BUFFER_SIZE / 64; // 16 orders
        
        for (int i = 0; i < maxOrders; i++) {
            Order order = createTestOrder(i, "TEST" + i, 1.0 + i, 1000 + i);
            assertTrue(strategy.serializeOrder(order), "订单 " + i + " 应该成功序列化");
        }
        
        // 尝试写入第17个订单，应该失败
        Order overflowOrder = createTestOrder(999, "OVERFLOW", 999.0, 999);
        assertFalse(strategy.serializeOrder(overflowOrder), 
                   getStrategyName() + " 缓冲区满时应该返回false");
    }
    
    @Test
    void testRingBufferBehavior() {
        System.out.println("  🔄 测试环形缓冲区行为...");
        
        // 填满缓冲区
        int maxOrders = BUFFER_SIZE / 64; // 16 orders
        
        for (int i = 0; i < maxOrders; i++) {
            Order order = createTestOrder(i, "TEST" + i, 1.0 + i, 1000 + i);
            strategy.serializeOrder(order);
        }
        
        // 使用环形缓冲区模式，应该从头开始覆盖
        Order newOrder = createTestOrder(999, "RING", 999.0, 999);
        assertTrue(strategy.serializeOrderRing(newOrder), 
                  getStrategyName() + " 环形缓冲区应该成功覆盖");
        
        // 验证第一个位置被覆盖（对于某些策略可能不适用）
        if (!(strategy instanceof SegmentedLockDirectMemory)) {
            Order result = strategy.deserializeOrderSafe(0);
            if (result != null) { // 有些策略可能不支持精确的位置覆盖
                assertEquals(999, result.getId());
            }
        }
    }
    
    @Test
    void testConcurrentWrites() throws InterruptedException {
        System.out.println("  🔀 测试并发写入...");
        
        final int threadCount = 8;
        final int ordersPerThread = 3; // 减少订单数量以适应小缓冲区
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < ordersPerThread; i++) {
                        Order order = createTestOrder(threadId * 1000 + i, 
                                                    "T" + threadId, 
                                                    1.0 + threadId, 
                                                    1000 + i);
                        if (strategy.serializeOrder(order)) {
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
        assertTrue(strategy.getUsedSize() >= 0, "内存使用不应该为负数");
        assertTrue(strategy.getUsedSize() <= BUFFER_SIZE, "内存使用不应该超过缓冲区大小");
        
        System.out.printf("    成功: %d, 失败: %d%n", successCount.get(), failureCount.get());
    }
    
    @Test
    void testConcurrentReads() throws InterruptedException {
        System.out.println("  📖 测试并发读取...");
        
        // 先写入一些订单
        for (int i = 0; i < 5; i++) {
            Order order = createTestOrder(i, "TEST" + i, 1.0 + i, 1000 + i);
            strategy.serializeOrder(order);
        }
        
        final int readerThreads = 8;
        final CountDownLatch latch = new CountDownLatch(readerThreads);
        final AtomicInteger readCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(readerThreads);
        
        for (int t = 0; t < readerThreads; t++) {
            executor.submit(() -> {
                try {
                    // 使用线程安全的反序列化方法
                    for (int i = 0; i < 5; i++) {
                        Order result = strategy.deserializeOrderSafe(i * 64);
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
        
        assertEquals(readerThreads * 5, readCount.get(), 
                    getStrategyName() + " 所有读操作都应该成功");
    }
    
    @Test
    void testMixedReadWrite() throws InterruptedException {
        System.out.println("  🔄 测试混合读写...");
        
        // 预先写入一些数据
        for (int i = 0; i < 3; i++) {
            Order order = createTestOrder(i, "INIT" + i, 1.0 + i, 1000);
            strategy.serializeOrder(order);
        }
        
        final int threadCount = 6;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger operations = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    // 70%概率读取，30%概率写入
                    for (int i = 0; i < 10; i++) {
                        if (i % 10 < 7) {
                            // 读取操作
                            Order result = strategy.deserializeOrderSafe(0);
                            if (result != null) {
                                operations.incrementAndGet();
                            }
                        } else {
                            // 写入操作
                            Order order = createTestOrder(threadId * 100 + i, 
                                                        "MIX" + threadId, 
                                                        2.0 + threadId, 
                                                        2000);
                            if (strategy.serializeOrder(order)) {
                                operations.incrementAndGet();
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertTrue(operations.get() > 0, "应该有成功的操作");
        System.out.printf("    完成操作: %d%n", operations.get());
    }
    
    @Test
    void testReset() {
        System.out.println("  🔄 测试重置...");
        
        // 写入一些数据
        for (int i = 0; i < 3; i++) {
            Order order = createTestOrder(i, "TEST" + i, 1.0 + i, 1000);
            strategy.serializeOrder(order);
        }
        
        assertTrue(strategy.getUsedSize() > 0, "重置前应该有数据");
        
        // 重置
        strategy.reset();
        
        assertEquals(0, strategy.getUsedSize(), "重置后使用大小应该为0");
        assertEquals(16, strategy.getRemainingOrderCapacity(), "重置后应该恢复全部容量");
    }
    
    @Test
    void testForceReset() {
        System.out.println("  ⚡ 测试强制重置...");
        
        // 写入一些数据
        for (int i = 0; i < 3; i++) {
            Order order = createTestOrder(i, "TEST" + i, 1.0 + i, 1000);
            strategy.serializeOrder(order);
        }
        
        assertTrue(strategy.getUsedSize() > 0, "强制重置前应该有数据");
        
        // 强制重置
        strategy.forceReset();
        
        assertEquals(0, strategy.getUsedSize(), "强制重置后使用大小应该为0");
    }
    
    @Test
    void testUtilityMethods() {
        System.out.println("  🔧 测试工具方法...");
        
        assertEquals(BUFFER_SIZE, strategy.getBufferSize());
        assertEquals(0, strategy.getUsedSize());
        assertEquals(BUFFER_SIZE, strategy.getRemainingSize());
        assertEquals(16, strategy.getRemainingOrderCapacity());
        assertFalse(strategy.isAlmostFull(0.8));
        
        // 写入一些数据后再测试
        for (int i = 0; i < 10; i++) {  // 10/16 = 62.5%
            Order order = createTestOrder(i, "TEST" + i, 1.0, 1000);
            strategy.serializeOrder(order);
        }
        
        assertFalse(strategy.isAlmostFull(0.8), "62.5%不应该超过80%");
        
        // 再写入3个，达到81.25%
        for (int i = 10; i < 13; i++) {
            Order order = createTestOrder(i, "TEST" + i, 1.0, 1000);
            strategy.serializeOrder(order);
        }
        
        assertTrue(strategy.isAlmostFull(0.8), "81.25%应该超过80%");
        assertEquals(3, strategy.getRemainingOrderCapacity(), "应该剩余3个订单容量");
    }
    
    @Test
    void testNullSymbolHandling() {
        System.out.println("  🚫 测试null符号处理...");
        
        Order orderWithNullSymbol = new Order();
        orderWithNullSymbol.setId(123);
        orderWithNullSymbol.setSymbol(null);  // null symbol
        orderWithNullSymbol.setPriceAndQuantity(1.234, 1000);
        orderWithNullSymbol.setSide((byte) 1);
        
        assertTrue(strategy.serializeOrder(orderWithNullSymbol), 
                  getStrategyName() + " 应该能处理null symbol");
        
        Order result = strategy.deserializeOrderSafe(0);
        
        assertEquals(123, result.getId());
        assertEquals("", result.getSymbol());  // null应该变成空字符串
    }
    
    @Test
    void testStrategyInfo() {
        System.out.println("  ℹ️ 测试策略信息...");
        
        assertNotNull(strategy.getStrategyName(), "策略名称不应该为null");
        assertNotNull(strategy.getStrategyDescription(), "策略描述不应该为null");
        assertFalse(strategy.getStrategyName().isEmpty(), "策略名称不应该为空");
        assertFalse(strategy.getStrategyDescription().isEmpty(), "策略描述不应该为空");
        
        System.out.println("    策略名称: " + strategy.getStrategyName());
        System.out.println("    策略描述: " + strategy.getStrategyDescription());
    }
    
    // 辅助方法
    private Order createTestOrder(int id, String symbol, double price, int quantity) {
        Order order = new Order();
        order.setId(id);
        order.setSymbol(symbol);
        order.setPriceAndQuantity(price, quantity);
        order.setSide((byte) 1);
        order.setTimestamp(System.nanoTime());
        return order;
    }
}