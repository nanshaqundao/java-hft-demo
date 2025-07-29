package com.hft.memory.benchmark;

import com.hft.memory.core.Order;
import com.hft.memory.memory.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * DirectMemoryStrategy 4种并发策略的性能基准测试
 * 
 * 测试场景：
 * 1. 单线程写入性能对比
 * 2. 多线程并发写入性能对比  
 * 3. 单线程读取性能对比
 * 4. 多线程并发读取性能对比
 * 5. 混合读写性能对比
 * 6. 批量操作性能对比
 * 
 * 策略对比：
 * - SynchronizedDirectMemory: 传统synchronized
 * - CASDirectMemory: 纯CAS无锁实现
 * - ReadWriteLockDirectMemory: 读写锁分离
 * - SegmentedLockDirectMemory: 分段锁实现
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class DirectMemoryStrategyBenchmark {
    
    // 测试配置
    private static final int BUFFER_SIZE = 64 * 1024 * 1024; // 64MB
    private static final int BATCH_SIZE = 100;
    private static final String[] SYMBOLS = {"EURUSD", "GBPUSD", "USDJPY", "AUDUSD", "USDCAD"};
    
    // 4种策略实例
    private SynchronizedDirectMemory synchronizedStrategy;
    private CASDirectMemory casStrategy;
    private ReadWriteLockDirectMemory rwLockStrategy;
    private SegmentedLockDirectMemory segmentedStrategy;
    
    // 测试数据
    private List<Order> testOrders;
    private Order reusableOrder;
    
    @Setup(Level.Trial)
    public void setupTrial() {
        // 初始化4种策略
        synchronizedStrategy = new SynchronizedDirectMemory(BUFFER_SIZE);
        casStrategy = new CASDirectMemory(BUFFER_SIZE);
        rwLockStrategy = new ReadWriteLockDirectMemory(BUFFER_SIZE);
        segmentedStrategy = new SegmentedLockDirectMemory(BUFFER_SIZE);
        
        // 生成测试数据
        testOrders = generateTestOrders(10000);
        reusableOrder = new Order();
        
        System.out.println("=== DirectMemoryStrategy Benchmark Setup ===");
        System.out.println("Buffer size: " + BUFFER_SIZE / 1024 / 1024 + "MB");
        System.out.println("Test orders: " + testOrders.size());
        System.out.println("Strategies initialized:");
        System.out.println("  - " + synchronizedStrategy.getStrategyName() + ": " + synchronizedStrategy.getStrategyDescription());
        System.out.println("  - " + casStrategy.getStrategyName() + ": " + casStrategy.getStrategyDescription());
        System.out.println("  - " + rwLockStrategy.getStrategyName() + ": " + rwLockStrategy.getStrategyDescription());
        System.out.println("  - " + segmentedStrategy.getStrategyName() + ": " + segmentedStrategy.getStrategyDescription());
    }
    
    @Setup(Level.Iteration)
    public void setupIteration() {
        // 每次迭代前重置所有策略
        synchronizedStrategy.reset();
        casStrategy.reset();
        rwLockStrategy.reset();
        segmentedStrategy.reset();
        
        // 建议GC运行，减少GC对测试的影响
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // ================================
    // 1. 单线程写入性能测试
    // ================================
    
    @Benchmark
    @Threads(1)
    public boolean singleThreadWrite_Synchronized(Blackhole bh) {
        Order order = testOrders.get(0);
        boolean result = synchronizedStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(1)
    public boolean singleThreadWrite_CAS(Blackhole bh) {
        Order order = testOrders.get(0);
        boolean result = casStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(1)
    public boolean singleThreadWrite_ReadWriteLock(Blackhole bh) {
        Order order = testOrders.get(0);
        boolean result = rwLockStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(1)
    public boolean singleThreadWrite_SegmentedLock(Blackhole bh) {
        Order order = testOrders.get(0);
        boolean result = segmentedStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    // ================================
    // 2. 多线程并发写入性能测试
    // ================================
    
    @Benchmark
    @Threads(8)
    public boolean multiThreadWrite_Synchronized(Blackhole bh) {
        Order order = testOrders.get(Thread.currentThread().hashCode() % testOrders.size());
        boolean result = synchronizedStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(8)
    public boolean multiThreadWrite_CAS(Blackhole bh) {
        Order order = testOrders.get(Thread.currentThread().hashCode() % testOrders.size());
        boolean result = casStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(8)
    public boolean multiThreadWrite_ReadWriteLock(Blackhole bh) {
        Order order = testOrders.get(Thread.currentThread().hashCode() % testOrders.size());
        boolean result = rwLockStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(8)
    public boolean multiThreadWrite_SegmentedLock(Blackhole bh) {
        Order order = testOrders.get(Thread.currentThread().hashCode() % testOrders.size());
        boolean result = segmentedStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    // ================================
    // 3. 单线程读取性能测试
    // ================================
    
    @Setup(Level.Invocation)
    public void setupReads() {
        // 为读取测试预先写入一些数据
        for (int i = 0; i < 100; i++) {
            Order order = testOrders.get(i % testOrders.size());
            synchronizedStrategy.serializeOrder(order);
            casStrategy.serializeOrder(order);
            rwLockStrategy.serializeOrder(order);
            segmentedStrategy.serializeOrder(order);
        }
    }
    
    @Benchmark
    @Threads(1)
    public Order singleThreadRead_Synchronized(Blackhole bh) {
        Order result = synchronizedStrategy.deserializeOrderSafe(64); // 读取第二个订单
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(1)
    public Order singleThreadRead_CAS(Blackhole bh) {
        Order result = casStrategy.deserializeOrderSafe(64);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(1)
    public Order singleThreadRead_ReadWriteLock(Blackhole bh) {
        Order result = rwLockStrategy.deserializeOrderSafe(64);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(1)
    public Order singleThreadRead_SegmentedLock(Blackhole bh) {
        Order result = segmentedStrategy.deserializeOrderSafe(64);
        bh.consume(result);
        return result;
    }
    
    // ================================
    // 4. 多线程并发读取性能测试
    // ================================
    
    @Benchmark
    @Threads(8)
    public Order multiThreadRead_Synchronized(Blackhole bh) {
        int offset = (Thread.currentThread().hashCode() % 100) * 64; // 读取不同位置
        Order result = synchronizedStrategy.deserializeOrderSafe(offset);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(8)
    public Order multiThreadRead_CAS(Blackhole bh) {
        int offset = (Thread.currentThread().hashCode() % 100) * 64;
        Order result = casStrategy.deserializeOrderSafe(offset);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(8)
    public Order multiThreadRead_ReadWriteLock(Blackhole bh) {
        int offset = (Thread.currentThread().hashCode() % 100) * 64;
        Order result = rwLockStrategy.deserializeOrderSafe(offset);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(8)
    public Order multiThreadRead_SegmentedLock(Blackhole bh) {
        int offset = (Thread.currentThread().hashCode() % 100) * 64;
        Order result = segmentedStrategy.deserializeOrderSafe(offset);
        bh.consume(result);
        return result;
    }
    
    // ================================
    // 5. 混合读写性能测试（7读:3写）
    // ================================
    
    @Benchmark
    @Threads(8)
    public Object mixedReadWrite_Synchronized(Blackhole bh) {
        // 70%概率读取，30%概率写入
        if (Thread.currentThread().hashCode() % 10 < 7) {
            // 读取操作
            int offset = Math.abs(Thread.currentThread().hashCode() % 50) * 64;
            Order result = synchronizedStrategy.deserializeOrderSafe(offset);
            bh.consume(result);
            return result;
        } else {
            // 写入操作
            Order order = testOrders.get(Math.abs(Thread.currentThread().hashCode()) % testOrders.size());
            boolean result = synchronizedStrategy.serializeOrder(order);
            bh.consume(result);
            return result;
        }
    }
    
    @Benchmark
    @Threads(8)
    public Object mixedReadWrite_CAS(Blackhole bh) {
        if (Thread.currentThread().hashCode() % 10 < 7) {
            int offset = Math.abs(Thread.currentThread().hashCode() % 50) * 64;
            Order result = casStrategy.deserializeOrderSafe(offset);
            bh.consume(result);
            return result;
        } else {
            Order order = testOrders.get(Math.abs(Thread.currentThread().hashCode()) % testOrders.size());
            boolean result = casStrategy.serializeOrder(order);
            bh.consume(result);
            return result;
        }
    }
    
    @Benchmark
    @Threads(8)
    public Object mixedReadWrite_ReadWriteLock(Blackhole bh) {
        if (Thread.currentThread().hashCode() % 10 < 7) {
            int offset = Math.abs(Thread.currentThread().hashCode() % 50) * 64;
            Order result = rwLockStrategy.deserializeOrderSafe(offset);
            bh.consume(result);
            return result;
        } else {
            Order order = testOrders.get(Math.abs(Thread.currentThread().hashCode()) % testOrders.size());
            boolean result = rwLockStrategy.serializeOrder(order);
            bh.consume(result);
            return result;
        }
    }
    
    @Benchmark
    @Threads(8)
    public Object mixedReadWrite_SegmentedLock(Blackhole bh) {
        if (Thread.currentThread().hashCode() % 10 < 7) {
            int offset = Math.abs(Thread.currentThread().hashCode() % 50) * 64;
            Order result = segmentedStrategy.deserializeOrderSafe(offset);
            bh.consume(result);
            return result;
        } else {
            Order order = testOrders.get(Math.abs(Thread.currentThread().hashCode()) % testOrders.size());
            boolean result = segmentedStrategy.serializeOrder(order);
            bh.consume(result);
            return result;
        }
    }
    
    // ================================
    // 6. 批量操作性能测试
    // ================================
    
    @Benchmark
    @Threads(1)
    public int batchWrite_Synchronized(Blackhole bh) {
        List<Order> batch = testOrders.subList(0, BATCH_SIZE);
        int result = synchronizedStrategy.serializeOrderBatch(batch);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(1)
    public int batchWrite_CAS(Blackhole bh) {
        List<Order> batch = testOrders.subList(0, BATCH_SIZE);
        int result = casStrategy.serializeOrderBatch(batch);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(1)
    public int batchWrite_ReadWriteLock(Blackhole bh) {
        List<Order> batch = testOrders.subList(0, BATCH_SIZE);
        int result = rwLockStrategy.serializeOrderBatch(batch);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(1)
    public int batchWrite_SegmentedLock(Blackhole bh) {
        List<Order> batch = testOrders.subList(0, BATCH_SIZE);
        int result = segmentedStrategy.serializeOrderBatch(batch);
        bh.consume(result);
        return result;
    }
    
    // ================================
    // 7. 环形缓冲区性能测试
    // ================================
    
    @Benchmark
    @Threads(4)
    public boolean ringBuffer_Synchronized(Blackhole bh) {
        Order order = testOrders.get(Math.abs(Thread.currentThread().hashCode()) % testOrders.size());
        boolean result = synchronizedStrategy.serializeOrderRing(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(4)
    public boolean ringBuffer_CAS(Blackhole bh) {
        Order order = testOrders.get(Math.abs(Thread.currentThread().hashCode()) % testOrders.size());
        boolean result = casStrategy.serializeOrderRing(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(4)
    public boolean ringBuffer_ReadWriteLock(Blackhole bh) {
        Order order = testOrders.get(Math.abs(Thread.currentThread().hashCode()) % testOrders.size());
        boolean result = rwLockStrategy.serializeOrderRing(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(4)
    public boolean ringBuffer_SegmentedLock(Blackhole bh) {
        Order order = testOrders.get(Math.abs(Thread.currentThread().hashCode()) % testOrders.size());
        boolean result = segmentedStrategy.serializeOrderRing(order);
        bh.consume(result);
        return result;
    }
    
    // ================================
    // 辅助方法
    // ================================
    
    /**
     * 生成测试订单数据
     */
    private List<Order> generateTestOrders(int count) {
        List<Order> orders = new ArrayList<>(count);
        Random random = new Random(42); // 固定种子确保可重复性
        
        for (int i = 0; i < count; i++) {
            String symbol = SYMBOLS[random.nextInt(SYMBOLS.length)];
            double price = 1.0 + random.nextDouble();
            int quantity = 1000 + random.nextInt(9000);
            byte side = (byte) (random.nextBoolean() ? 1 : 2);
            
            Order order = new Order();
            order.setId(i);
            order.setSymbol(symbol);
            order.setPriceAndQuantity(price, quantity);
            order.setSide(side);
            order.setTimestamp(System.nanoTime());
            
            orders.add(order);
        }
        
        return orders;
    }
    
    @TearDown(Level.Trial)
    public void tearDown() {
        System.out.println("=== Benchmark Complete ===");
        System.out.println("Strategy Performance Summary:");
        System.out.printf("  - %s: Used=%d bytes, Remaining=%d bytes%n", 
            synchronizedStrategy.getStrategyName(),
            synchronizedStrategy.getUsedSize(),
            synchronizedStrategy.getRemainingSize());
        System.out.printf("  - %s: Used=%d bytes, Remaining=%d bytes%n", 
            casStrategy.getStrategyName(),
            casStrategy.getUsedSize(),
            casStrategy.getRemainingSize());
        System.out.printf("  - %s: Used=%d bytes, Remaining=%d bytes%n", 
            rwLockStrategy.getStrategyName(),
            rwLockStrategy.getUsedSize(),
            rwLockStrategy.getRemainingSize());
        System.out.printf("  - %s: Used=%d bytes, Remaining=%d bytes%n", 
            segmentedStrategy.getStrategyName(),
            segmentedStrategy.getUsedSize(),
            segmentedStrategy.getRemainingSize());
    }
    
    // 运行基准测试
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(DirectMemoryStrategyBenchmark.class.getSimpleName())
                .shouldDoGC(true)
                .jvmArgs("-Xmx4g", "-Xms4g", 
                        "-XX:+UseG1GC", 
                        "-XX:MaxGCPauseMillis=10",
                        "-XX:+UnlockExperimentalVMOptions",
                        "-XX:+UseZGC") // 使用低延迟GC
                .build();
        
        new Runner(opt).run();
    }
}