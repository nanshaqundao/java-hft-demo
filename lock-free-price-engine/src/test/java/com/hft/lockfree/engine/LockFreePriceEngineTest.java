package com.hft.lockfree.engine;

import com.hft.lockfree.handler.AggregationHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LockFreePriceEngine集成测试
 */
class LockFreePriceEngineTest {
    
    private LockFreePriceEngine engine;
    
    @BeforeEach
    void setUp() {
        engine = new LockFreePriceEngine();
    }
    
    @AfterEach
    void tearDown() {
        if (engine != null && engine.isRunning()) {
            engine.shutdown();
        }
    }
    
    @Test
    void testEngineLifecycle() {
        // Initially not running
        assertFalse(engine.isRunning());
        
        // Start engine
        engine.start();
        assertTrue(engine.isRunning());
        
        // Stop engine
        engine.shutdown();
        assertFalse(engine.isRunning());
    }
    
    @Test
    void testDoubleStart() {
        engine.start();
        assertTrue(engine.isRunning());
        
        // Second start should be ignored
        engine.start();
        assertTrue(engine.isRunning());
        
        engine.shutdown();
        assertFalse(engine.isRunning());
    }
    
    @Test
    void testDoubleShutdown() {
        engine.start();
        engine.shutdown();
        assertFalse(engine.isRunning());
        
        // Second shutdown should be ignored
        engine.shutdown();
        assertFalse(engine.isRunning());
    }
    
    @Test
    void testPublishPriceWhenStopped() {
        // Engine not started
        assertFalse(engine.isRunning());
        
        boolean result = engine.publishPrice("EURUSD", 1.0999, 1.1001);
        assertFalse(result);
    }
    
    @Test
    void testPublishPriceWhenRunning() throws InterruptedException {
        engine.start();
        assertTrue(engine.isRunning());
        
        boolean result = engine.publishPrice("EURUSD", 1.0999, 1.1001);
        assertTrue(result);
        
        // Wait for processing
        Thread.sleep(100);
        
        // Verify event was processed
        assertTrue(engine.getPublishedEventCount() > 0);
    }
    
    @Test
    void testMultiplePricePublish() throws InterruptedException {
        engine.start();
        
        String[] symbols = {"EURUSD", "GBPUSD", "USDJPY"};
        
        for (int i = 0; i < 100; i++) {
            String symbol = symbols[i % symbols.length];
            double basePrice = 1.0 + i * 0.0001;
            
            boolean result = engine.publishPrice(symbol, basePrice - 0.0001, basePrice + 0.0001);
            assertTrue(result);
        }
        
        // Wait for processing
        Thread.sleep(200);
        
        assertEquals(100, engine.getPublishedEventCount());
        
        // Check aggregation results
        AggregationHandler aggregationHandler = engine.getAggregationHandler();
        for (String symbol : symbols) {
            var snapshot = aggregationHandler.getLatestPrice(symbol);
            assertNotNull(snapshot, "Should have price data for " + symbol);
            assertTrue(snapshot.getUpdateCount() > 0);
        }
    }
    
    @Test
    void testPublishPriceWithTimestamp() throws InterruptedException {
        engine.start();
        
        long timestamp = System.nanoTime();
        boolean result = engine.publishPrice("EURUSD", 1.0999, 1.1001, timestamp);
        assertTrue(result);
        
        Thread.sleep(100);
        
        AggregationHandler aggregationHandler = engine.getAggregationHandler();
        var snapshot = aggregationHandler.getLatestPrice("EURUSD");
        assertNotNull(snapshot);
        assertEquals(timestamp, snapshot.getTimestamp());
    }
    
    @Test
    void testStatistics() throws InterruptedException {
        engine.start();
        
        // Publish some events
        for (int i = 0; i < 10; i++) {
            engine.publishPrice("EURUSD", 1.0999, 1.1001);
        }
        
        Thread.sleep(100);
        
        String statistics = engine.getStatistics();
        assertNotNull(statistics);
        assertTrue(statistics.contains("published=10"));
        assertTrue(statistics.contains("ValidationHandler"));
        assertTrue(statistics.contains("AggregationHandler"));
        assertTrue(statistics.contains("DistributionHandler"));
        assertTrue(statistics.contains("RingBuffer"));
    }
    
    @Test
    void testResetStatistics() throws InterruptedException {
        engine.start();
        
        // Publish some events
        for (int i = 0; i < 5; i++) {
            engine.publishPrice("EURUSD", 1.0999, 1.1001);
        }
        
        Thread.sleep(100);
        assertTrue(engine.getPublishedEventCount() > 0);
        
        // Reset statistics
        engine.resetStatistics();
        assertEquals(0, engine.getPublishedEventCount());
    }
    
    @Test
    void testRingBufferStatus() {
        engine.start();
        
        String status = engine.getRingBufferStatus();
        assertNotNull(status);
        assertTrue(status.contains("RingBuffer"));
        assertTrue(status.contains("size="));
        assertTrue(status.contains("used="));
        assertTrue(status.contains("remaining="));
    }
    
    @Test
    void testInvalidRingBufferSize() {
        // Ring buffer size must be power of 2
        assertThrows(IllegalArgumentException.class, () -> {
            new LockFreePriceEngine(1000, null); // 1000 is not power of 2
        });
    }
    
    @Test
    void testValidRingBufferSizes() {
        // These should not throw exceptions
        var engine1 = new LockFreePriceEngine(1024, new com.lmax.disruptor.YieldingWaitStrategy());
        assertNotNull(engine1);
        
        var engine2 = new LockFreePriceEngine(4096, new com.lmax.disruptor.BlockingWaitStrategy());
        assertNotNull(engine2);
        
        var engine3 = new LockFreePriceEngine(16384, new com.lmax.disruptor.SleepingWaitStrategy());
        assertNotNull(engine3);
    }
}