package com.hft.lockfree.benchmark;

import com.hft.lockfree.engine.LockFreePriceEngine;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 快速基准测试
 * 
 * 用于开发阶段的快速性能验证，运行时间短，迭代次数少
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class QuickBenchmark {
    
    private LockFreePriceEngine engine;
    private final String[] symbols = {"EURUSD", "GBPUSD", "USDJPY"};
    private final double[] basePrices = {1.1000, 1.3000, 110.00};
    
    @Setup(Level.Trial)
    public void setup() {
        engine = new LockFreePriceEngine();
        engine.start();
        
        // 简单预热
        for (int i = 0; i < 1000; i++) {
            engine.publishPrice("EURUSD", 1.0999, 1.1001);
        }
        
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        engine.resetStatistics();
    }
    
    @TearDown(Level.Trial)
    public void teardown() {
        if (engine != null) {
            engine.shutdown();
        }
    }
    
    /**
     * 快速吞吐量测试
     */
    @Benchmark
    public void quickThroughputTest(Blackhole bh) {
        int index = ThreadLocalRandom.current().nextInt(symbols.length);
        double basePrice = basePrices[index];
        
        boolean result = engine.publishPrice(symbols[index], 
                basePrice - 0.0001, basePrice + 0.0001);
        bh.consume(result);
    }
    
    /**
     * 快速延迟测试
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public void quickLatencyTest(Blackhole bh) {
        boolean result = engine.publishPrice("EURUSD", 1.0999, 1.1001);
        bh.consume(result);
    }
    
    /**
     * 快速查询测试
     */
    @Benchmark
    public void quickQueryTest(Blackhole bh) {
        var snapshot = engine.getAggregationHandler().getLatestPrice("EURUSD");
        bh.consume(snapshot);
    }
}