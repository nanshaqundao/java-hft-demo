package com.hft.memory.core;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

/**
 * Java 21 Virtual Thread-based Order Processor
 * Demonstrates massive concurrency with lightweight threads
 */
public class VirtualThreadOrderProcessor {
    
    private final ExecutorService virtualThreadExecutor;
    private final MemoryOptimizedOrderProcessor processor;
    
    public VirtualThreadOrderProcessor() {
        // Create virtual thread executor (Java 21 feature)
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.processor = new MemoryOptimizedOrderProcessor();
    }
    
    /**
     * Process orders concurrently using Virtual Threads
     * Each order gets its own virtual thread for maximum parallelism
     */
    public CompletableFuture<List<ProcessingResult>> processOrdersConcurrently(List<OrderData> orders) {
        return CompletableFuture.supplyAsync(() -> {
            var futures = orders.stream()
                .map(orderData -> CompletableFuture.supplyAsync(
                    () -> processor.processOrder(orderData), 
                    virtualThreadExecutor))
                .toList();
            
            // Wait for all to complete and collect results
            return futures.stream()
                .map(CompletableFuture::join)
                .toList();
        }, virtualThreadExecutor);
    }
    
    /**
     * Batch process with virtual thread pools
     * Splits large batches across multiple virtual threads
     */
    public CompletableFuture<BatchProcessingResult> processBatchWithVirtualThreads(
            List<OrderData> orders, int batchSize) {
        
        return CompletableFuture.supplyAsync(() -> {
            var batchFutures = IntStream.range(0, (orders.size() + batchSize - 1) / batchSize)
                .mapToObj(i -> {
                    int start = i * batchSize;
                    int end = Math.min(start + batchSize, orders.size());
                    List<OrderData> batch = orders.subList(start, end);
                    
                    return CompletableFuture.supplyAsync(
                        () -> processor.processOrdersBatch(batch),
                        virtualThreadExecutor);
                })
                .toList();
            
            // Combine all batch results
            var results = batchFutures.stream()
                .map(CompletableFuture::join)
                .toList();
            
            // Aggregate results
            int totalSuccess = results.stream().mapToInt(BatchProcessingResult::getSuccessCount).sum();
            int totalFailure = results.stream().mapToInt(BatchProcessingResult::getFailureCount).sum();
            long totalTime = results.stream().mapToLong(BatchProcessingResult::getTotalTimeNs).sum();
            int totalSerialized = results.stream().mapToInt(BatchProcessingResult::getSerializedCount).sum();
            
            return new BatchProcessingResult(totalSuccess, totalFailure, totalTime, totalSerialized);
            
        }, virtualThreadExecutor);
    }
    
    /**
     * Simulate high-frequency trading scenario with thousands of concurrent orders
     */
    public CompletableFuture<PerformanceStats> simulateHighFrequencyTrading(int orderCount) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            
            // Generate orders
            var orders = IntStream.range(0, orderCount)
                .mapToObj(i -> new OrderData(i, "EURUSD", 1.1 + Math.random() * 0.1, 
                    1000 + (int)(Math.random() * 9000), (byte)(Math.random() > 0.5 ? 1 : 2)))
                .toList();
            
            // Process with virtual threads
            var results = processOrdersConcurrently(orders).join();
            
            long totalTime = System.nanoTime() - startTime;
            long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
            double avgLatency = results.stream().mapToLong(ProcessingResult::getProcessingTimeNs).average().orElse(0.0);
            double throughput = successCount * 1_000_000_000.0 / totalTime;
            
            return new PerformanceStats(successCount, avgLatency, throughput, 
                processor.getOrderCache().size(), processor.getOrderPool().size());
                
        }, virtualThreadExecutor);
    }
    
    /**
     * Pattern matching example with sealed interface (Java 21)
     */
    public String formatOrderResult(ProcessingResult result) {
        return switch (result) {
            case ProcessingResult r when r.isSuccess() && r.isSerialized() -> 
                "SUCCESS: Processed and serialized in %.2fμs".formatted(r.getProcessingTimeNs() / 1000.0);
            case ProcessingResult r when r.isSuccess() && !r.isSerialized() ->
                "PARTIAL: Processed but not serialized in %.2fμs".formatted(r.getProcessingTimeNs() / 1000.0);
            case ProcessingResult r when !r.isSuccess() ->
                "FAILED: %s after %.2fμs".formatted(
                    r.getError() != null ? r.getError().getMessage() : "Unknown error",
                    r.getProcessingTimeNs() / 1000.0);
            default -> "UNKNOWN_RESULT";
        };
    }
    
    public void shutdown() {
        processor.shutdown();
        virtualThreadExecutor.shutdown();
    }
    
    public MemoryOptimizedOrderProcessor getProcessor() {
        return processor;
    }
}