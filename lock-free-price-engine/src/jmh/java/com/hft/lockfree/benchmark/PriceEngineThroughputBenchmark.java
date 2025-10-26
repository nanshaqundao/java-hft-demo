package com.hft.lockfree.benchmark;

import com.hft.lockfree.engine.LockFreePriceEngine;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 价格引擎吞吐量基准测试
 * 
 * 测试目标：
 * - 不同等待策略的吞吐量对比
 * - 不同RingBuffer大小的影响
 * - 持续负载下的性能表现
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class PriceEngineThroughputBenchmark {
    
    @Param({"YieldingWaitStrategy", "BlockingWaitStrategy", "SleepingWaitStrategy", "BusySpinWaitStrategy"})
    public String waitStrategyName;
    
    @Param({"16384", "65536", "262144"})  // 16K, 64K, 256K
    public int ringBufferSize;
    
    private LockFreePriceEngine engine;
    private String[] symbols;
    private double[] basePrices;
    
    @Setup(Level.Trial)
    public void setupTrial() {
        // 根据参数创建等待策略
        var waitStrategy = switch (waitStrategyName) {
            case "YieldingWaitStrategy" -> new YieldingWaitStrategy();
            case "BlockingWaitStrategy" -> new BlockingWaitStrategy();
            case "SleepingWaitStrategy" -> new SleepingWaitStrategy();
            case "BusySpinWaitStrategy" -> new BusySpinWaitStrategy();
            default -> throw new IllegalArgumentException("Unknown wait strategy: " + waitStrategyName);
        };
        
        // 创建引擎
        engine = new LockFreePriceEngine(ringBufferSize, waitStrategy);
        engine.start();
        
        // 准备测试数据
        symbols = new String[]{"EURUSD", "GBPUSD", "USDJPY", "AUDUSD", "USDCAD", 
                              "EURGBP", "EURJPY", "GBPJPY", "XAUUSD", "USOIL"};
        basePrices = new double[symbols.length];
        
        for (int i = 0; i < symbols.length; i++) {
            basePrices[i] = 1.0 + ThreadLocalRandom.current().nextDouble() * 0.5;
        }
        
        // 预热
        warmupEngine();
    }
    
    @TearDown(Level.Trial)
    public void teardownTrial() {
        if (engine != null) {
            engine.shutdown();
        }
    }
    
    private void warmupEngine() {
        for (int i = 0; i < 50000; i++) {
            int index = i % symbols.length;
            double basePrice = basePrices[index];
            engine.publishPrice(symbols[index], basePrice - 0.0001, basePrice + 0.0001);
        }
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 重置统计
        engine.resetStatistics();
    }
    
    /**
     * 测试连续价格发布吞吐量
     */
    @Benchmark
    public void continuousPricePublish(Blackhole bh) {
        int index = ThreadLocalRandom.current().nextInt(symbols.length);
        double basePrice = basePrices[index];
        double spread = 0.0001 + ThreadLocalRandom.current().nextDouble() * 0.0005;
        double bidPrice = basePrice - spread / 2;
        double askPrice = basePrice + spread / 2;
        
        boolean result = engine.publishPrice(symbols[index], bidPrice, askPrice);
        bh.consume(result);
    }
    
    /**
     * 测试混合操作吞吐量（90%写入，10%查询）
     */
    @Benchmark
    @OperationsPerInvocation(10)
    public void mixedOperations(Blackhole bh) {
        for (int i = 0; i < 10; i++) {
            if (i == 9) { // 10%的查询操作
                String symbol = symbols[ThreadLocalRandom.current().nextInt(symbols.length)];
                var snapshot = engine.getAggregationHandler().getLatestPrice(symbol);
                bh.consume(snapshot);
            } else { // 90%的写入操作
                int index = ThreadLocalRandom.current().nextInt(symbols.length);
                double basePrice = basePrices[index];
                double spread = 0.0001 + ThreadLocalRandom.current().nextDouble() * 0.0005;
                double bidPrice = basePrice - spread / 2;
                double askPrice = basePrice + spread / 2;
                
                boolean result = engine.publishPrice(symbols[index], bidPrice, askPrice);
                bh.consume(result);
            }
        }
    }
    
    /**
     * 测试批量价格更新吞吐量
     */
    @Benchmark
    @OperationsPerInvocation(50)
    public void batchPriceUpdates(Blackhole bh) {
        for (int i = 0; i < 50; i++) {
            int index = i % symbols.length;
            double basePrice = basePrices[index];
            
            // 模拟价格波动
            double volatility = ThreadLocalRandom.current().nextGaussian() * 0.001;
            double newPrice = basePrice + volatility;
            double spread = 0.0001;
            
            boolean result = engine.publishPrice(symbols[index], 
                    newPrice - spread, newPrice + spread);
            bh.consume(result);
        }
    }
}