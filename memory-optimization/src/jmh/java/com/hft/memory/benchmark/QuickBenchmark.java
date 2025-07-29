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
 * 快速基准测试 - 专为时间限制环境设计
 * 
 * 特点：
 * - 极简配置：1次预热，2次测量，每次2秒
 * - 核心场景：单线程写入、多线程写入、读取性能
 * - 总耗时：约1-2分钟
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class QuickBenchmark {
    
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB，更小的缓冲区
    private static final String[] SYMBOLS = {"EURUSD", "GBPUSD", "USDJPY"};
    
    // 4种策略实例
    private SynchronizedDirectMemory synchronizedStrategy;
    private CASDirectMemory casStrategy;
    private ReadWriteLockDirectMemory rwLockStrategy;
    private SegmentedLockDirectMemory segmentedStrategy;
    
    // 测试数据
    private List<Order> testOrders;
    
    @Setup(Level.Trial)
    public void setupTrial() {
        System.out.println("🚀 Quick Benchmark - 初始化中...");
        
        // 初始化4种策略
        synchronizedStrategy = new SynchronizedDirectMemory(BUFFER_SIZE);
        casStrategy = new CASDirectMemory(BUFFER_SIZE);
        rwLockStrategy = new ReadWriteLockDirectMemory(BUFFER_SIZE);
        segmentedStrategy = new SegmentedLockDirectMemory(BUFFER_SIZE);
        
        // 生成少量测试数据
        testOrders = generateTestOrders(100);
        
        System.out.println("  缓冲区大小: " + BUFFER_SIZE / 1024 + "KB");
        System.out.println("  测试数据: " + testOrders.size() + " 订单");
        System.out.println("  预计耗时: 1-2分钟");
    }
    
    @Setup(Level.Iteration)
    public void setupIteration() {
        // 每次迭代前重置
        synchronizedStrategy.reset();
        casStrategy.reset();
        rwLockStrategy.reset();
        segmentedStrategy.reset();
    }
    
    // ========================================
    // 核心性能测试 - 单线程写入
    // ========================================
    
    @Benchmark
    @Threads(1)
    public boolean write_Synchronized(Blackhole bh) {
        Order order = testOrders.get(0);
        boolean result = synchronizedStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(1)
    public boolean write_CAS(Blackhole bh) {
        Order order = testOrders.get(0);
        boolean result = casStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(1)
    public boolean write_ReadWriteLock(Blackhole bh) {
        Order order = testOrders.get(0);
        boolean result = rwLockStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(1)
    public boolean write_SegmentedLock(Blackhole bh) {
        Order order = testOrders.get(0);
        boolean result = segmentedStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    // ========================================
    // 并发性能测试 - 4线程写入
    // ========================================
    
    @Benchmark
    @Threads(4)
    public boolean concurrentWrite_Synchronized(Blackhole bh) {
        Order order = testOrders.get(Thread.currentThread().hashCode() % testOrders.size());
        boolean result = synchronizedStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(4)
    public boolean concurrentWrite_CAS(Blackhole bh) {
        Order order = testOrders.get(Thread.currentThread().hashCode() % testOrders.size());
        boolean result = casStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(4)
    public boolean concurrentWrite_ReadWriteLock(Blackhole bh) {
        Order order = testOrders.get(Thread.currentThread().hashCode() % testOrders.size());
        boolean result = rwLockStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    @Benchmark
    @Threads(4)
    public boolean concurrentWrite_SegmentedLock(Blackhole bh) {
        Order order = testOrders.get(Thread.currentThread().hashCode() % testOrders.size());
        boolean result = segmentedStrategy.serializeOrder(order);
        bh.consume(result);
        return result;
    }
    
    // ========================================
    // 辅助方法
    // ========================================
    
    private List<Order> generateTestOrders(int count) {
        List<Order> orders = new ArrayList<>(count);
        Random random = new Random(42);
        
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
        System.out.println("\n🎉 Quick Benchmark 完成！");
        System.out.println("📊 结果解读：");
        System.out.println("  - 数字越大 = 性能越好（每秒操作数）");
        System.out.println("  - 单线程: 基础性能对比");
        System.out.println("  - 4线程: 并发扩展性对比");
        System.out.println("\n💡 预期结果：");
        System.out.println("  - 单线程: CAS ≈ Synchronized > ReadWriteLock ≈ SegmentedLock");
        System.out.println("  - 4线程: SegmentedLock > CAS > ReadWriteLock > Synchronized");
    }
    
    // 快速运行入口
    public static void main(String[] args) throws Exception {
        System.out.println("🚀 启动快速基准测试...");
        
        Options opt = new OptionsBuilder()
                .include(QuickBenchmark.class.getSimpleName())
                .shouldDoGC(true)
                .jvmArgs("-Xmx1g", "-Xms1g") // 减少内存使用
                .build();
        
        new Runner(opt).run();
    }
}