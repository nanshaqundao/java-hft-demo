package com.hft.lockfree.benchmark;

import com.hft.lockfree.engine.LockFreePriceEngine;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 价格引擎延迟基准测试
 * 
 * 测试目标：
 * - 单次价格发布的延迟
 * - 不同等待策略的影响
 * - 内存分配对性能的影响
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class PriceEngineLatencyBenchmark {
    
    private LockFreePriceEngine engine;
    private String[] symbols;
    private double[] basePrices;
    
    @Setup(Level.Trial)
    public void setupTrial() {
        // 创建价格引擎
        engine = new LockFreePriceEngine();
        engine.start();
        
        // 预生成测试数据
        symbols = new String[]{"EURUSD", "GBPUSD", "USDJPY", "AUDUSD", "USDCAD"};
        basePrices = new double[symbols.length];
        
        for (int i = 0; i < symbols.length; i++) {
            basePrices[i] = 1.0 + ThreadLocalRandom.current().nextDouble() * 0.5;
        }
        
        // 预热引擎
        warmupEngine();
    }
    
    @TearDown(Level.Trial)
    public void teardownTrial() {
        if (engine != null) {
            engine.shutdown();
        }
    }
    
    /**
     * 预热引擎，确保JIT编译和缓存预热
     */
    private void warmupEngine() {
        for (int i = 0; i < 10000; i++) {
            int index = i % symbols.length;
            double basePrice = basePrices[index];
            engine.publishPrice(symbols[index], basePrice - 0.0001, basePrice + 0.0001);
        }
        
        // 等待处理完成
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 测试单次价格发布延迟
     */
    @Benchmark
    public void singlePricePublish(Blackhole bh) {
        int index = ThreadLocalRandom.current().nextInt(symbols.length);
        double basePrice = basePrices[index];
        double bidPrice = basePrice - 0.0001;
        double askPrice = basePrice + 0.0001;
        
        boolean result = engine.publishPrice(symbols[index], bidPrice, askPrice);
        bh.consume(result);
    }
    
    /**
     * 测试批量价格发布的平均延迟
     */
    @Benchmark
    @OperationsPerInvocation(10)
    public void batchPricePublish(Blackhole bh) {
        for (int i = 0; i < 10; i++) {
            int index = i % symbols.length;
            double basePrice = basePrices[index];
            double bidPrice = basePrice - 0.0001;
            double askPrice = basePrice + 0.0001;
            
            boolean result = engine.publishPrice(symbols[index], bidPrice, askPrice);
            bh.consume(result);
        }
    }
    
    /**
     * 测试带时间戳的价格发布延迟
     */
    @Benchmark
    public void pricePublishWithTimestamp(Blackhole bh) {
        int index = ThreadLocalRandom.current().nextInt(symbols.length);
        double basePrice = basePrices[index];
        double bidPrice = basePrice - 0.0001;
        double askPrice = basePrice + 0.0001;
        long timestamp = System.nanoTime();
        
        boolean result = engine.publishPrice(symbols[index], bidPrice, askPrice, timestamp);
        bh.consume(result);
    }
    
    /**
     * 测试价格查询延迟
     */
    @Benchmark
    public void priceQuery(Blackhole bh) {
        String symbol = symbols[ThreadLocalRandom.current().nextInt(symbols.length)];
        var snapshot = engine.getAggregationHandler().getLatestPrice(symbol);
        bh.consume(snapshot);
    }
}