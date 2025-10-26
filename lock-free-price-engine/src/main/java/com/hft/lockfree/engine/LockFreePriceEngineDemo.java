package com.hft.lockfree.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 无锁价格引擎演示程序
 * 
 * 演示：
 * 1. 引擎的基本启动和关闭
 * 2. 价格数据的发布
 * 3. 性能统计的查看
 */
public class LockFreePriceEngineDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(LockFreePriceEngineDemo.class);
    
    // 测试数据
    private static final String[] SYMBOLS = {"EURUSD", "GBPUSD", "USDJPY", "AUDUSD", "USDCAD"};
    private static final int EVENTS_TO_PUBLISH = 100_000;
    
    public static void main(String[] args) {
        logger.info("=== LockFreePriceEngine Demo Started ===");
        
        // 创建价格引擎
        LockFreePriceEngine engine = new LockFreePriceEngine();
        
        try {
            // 启动引擎
            engine.start();
            
            // 运行性能测试
            runPerformanceTest(engine);
            
            // 显示统计信息
            displayStatistics(engine);
            
            // 查询价格信息
            queryPrices(engine);
            
        } catch (Exception e) {
            logger.error("Demo execution failed: {}", e.getMessage(), e);
        } finally {
            // 关闭引擎
            engine.shutdown();
            logger.info("=== LockFreePriceEngine Demo Completed ===");
        }
    }
    
    /**
     * 运行性能测试
     */
    private static void runPerformanceTest(LockFreePriceEngine engine) {
        logger.info("Starting performance test with {} events...", EVENTS_TO_PUBLISH);
        
        long startTime = System.nanoTime();
        
        // 发布价格事件
        for (int i = 0; i < EVENTS_TO_PUBLISH; i++) {
            String symbol = SYMBOLS[i % SYMBOLS.length];
            double basePrice = 1.0 + ThreadLocalRandom.current().nextDouble() * 0.5;
            double bidPrice = basePrice - 0.0001;
            double askPrice = basePrice + 0.0001;
            
            boolean success = engine.publishPrice(symbol, bidPrice, askPrice);
            if (!success) {
                logger.warn("Failed to publish price for {}", symbol);
                break;
            }
            
            // 每10000个事件显示进度
            if ((i + 1) % 10000 == 0) {
                logger.info("Published {} events", i + 1);
            }
        }
        
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        
        // 等待处理完成（简单延迟）
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 计算性能指标
        double durationSeconds = duration / 1_000_000_000.0;
        double throughput = EVENTS_TO_PUBLISH / durationSeconds;
        double avgLatency = duration / (double) EVENTS_TO_PUBLISH;
        
        logger.info("Performance Test Results:");
        logger.info("  Events Published: {}", EVENTS_TO_PUBLISH);
        logger.info("  Duration: {:.3f} seconds", durationSeconds);
        logger.info("  Throughput: {:.0f} events/sec", throughput);
        logger.info("  Average Latency: {:.2f} ns/event", avgLatency);
    }
    
    /**
     * 显示统计信息
     */
    private static void displayStatistics(LockFreePriceEngine engine) {
        logger.info("\n=== Engine Statistics ===");
        String statistics = engine.getStatistics();
        for (String line : statistics.split("\n")) {
            logger.info(line);
        }
    }
    
    /**
     * 查询价格信息
     */
    private static void queryPrices(LockFreePriceEngine engine) {
        logger.info("\n=== Latest Prices ===");
        
        var aggregationHandler = engine.getAggregationHandler();
        
        for (String symbol : SYMBOLS) {
            var snapshot = aggregationHandler.getLatestPrice(symbol);
            if (snapshot != null) {
                logger.info("{}: {}", symbol, snapshot);
            } else {
                logger.info("{}: No data available", symbol);
            }
        }
    }
}