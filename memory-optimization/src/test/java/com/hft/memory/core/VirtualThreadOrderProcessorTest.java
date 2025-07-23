package com.hft.memory.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

class VirtualThreadOrderProcessorTest {
    
    private VirtualThreadOrderProcessor processor;
    private List<OrderData> testOrders;
    
    @BeforeEach
    void setUp() {
        processor = new VirtualThreadOrderProcessor();
        testOrders = generateTestOrders(100);
    }
    
    @AfterEach
    void tearDown() {
        processor.shutdown();
    }
    
    @Test
    void testConcurrentOrderProcessing() {
        CompletableFuture<List<ProcessingResult>> future = 
            processor.processOrdersConcurrently(testOrders.subList(0, 10));
        
        List<ProcessingResult> results = future.join();
        
        assertEquals(10, results.size());
        assertTrue(results.stream().allMatch(ProcessingResult::isSuccess));
    }
    
    @Test
    void testBatchProcessingWithVirtualThreads() {
        CompletableFuture<BatchProcessingResult> future = 
            processor.processBatchWithVirtualThreads(testOrders, 25);
        
        BatchProcessingResult result = future.join();
        
        assertEquals(100, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertTrue(result.getThroughputPerSec() > 0);
    }
    
    @Test
    void testHighFrequencyTradingSimulation() {
        CompletableFuture<PerformanceStats> future = 
            processor.simulateHighFrequencyTrading(1000);
        
        PerformanceStats stats = future.join();
        
        assertEquals(1000, stats.getTotalProcessed());
        assertTrue(stats.getThroughputPerSec() > 0);
        assertTrue(stats.getAvgLatencyNs() > 0);
    }
    
    @Test
    void testPatternMatchingFormatting() {
        ProcessingResult success = new ProcessingResult(true, 1500L, true);
        ProcessingResult partial = new ProcessingResult(true, 2000L, false);
        ProcessingResult failure = new ProcessingResult(false, 500L, false, 
            new RuntimeException("Test error"));
        
        String successMsg = processor.formatOrderResult(success);
        String partialMsg = processor.formatOrderResult(partial);
        String failureMsg = processor.formatOrderResult(failure);
        
        assertTrue(successMsg.contains("SUCCESS"));
        assertTrue(successMsg.contains("1.50μs"));
        
        assertTrue(partialMsg.contains("PARTIAL"));
        assertTrue(partialMsg.contains("2.00μs"));
        
        assertTrue(failureMsg.contains("FAILED"));
        assertTrue(failureMsg.contains("Test error"));
        assertTrue(failureMsg.contains("0.50μs"));
    }
    
    @Test
    void testVirtualThreadPerformance() {
        // This test verifies that virtual threads can handle many concurrent operations
        List<OrderData> manyOrders = generateTestOrders(1000);
        
        long startTime = System.nanoTime();
        CompletableFuture<List<ProcessingResult>> future = 
            processor.processOrdersConcurrently(manyOrders);
        List<ProcessingResult> results = future.join();
        long endTime = System.nanoTime();
        
        assertEquals(1000, results.size());
        assertTrue(results.stream().allMatch(ProcessingResult::isSuccess));
        
        // Virtual threads should complete this reasonably quickly
        long durationMs = (endTime - startTime) / 1_000_000;
        assertTrue(durationMs < 5000, "Processing took too long: " + durationMs + "ms");
    }
    
    private List<OrderData> generateTestOrders(int count) {
        List<OrderData> orders = new ArrayList<>(count);
        String[] symbols = {"EURUSD", "GBPUSD", "USDJPY"};
        
        for (int i = 0; i < count; i++) {
            String symbol = symbols[i % symbols.length];
            double price = 1.0 + (i % 100) / 1000.0;
            int quantity = 1000 + (i % 5000);
            byte side = (byte) ((i % 2) + 1);
            
            orders.add(new OrderData(i, symbol, price, quantity, side));
        }
        
        return orders;
    }
}