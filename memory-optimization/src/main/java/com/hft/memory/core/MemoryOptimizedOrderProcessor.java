package com.hft.memory.core;

import com.hft.memory.pool.ObjectPool;
import com.hft.memory.memory.DirectMemoryManager;
import com.hft.memory.cache.OrderCache;

import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;

public class MemoryOptimizedOrderProcessor {
    // Core components
    private final ObjectPool<Order> orderPool;
    private final DirectMemoryManager memoryManager;
    private final OrderCache orderCache;
    
    // Performance statistics
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    
    // Batch processing configuration
    private static final int BATCH_SIZE = 1000;
    private static final int MEMORY_BUFFER_SIZE = 64 * 1024 * 1024; // 64MB
    
    public MemoryOptimizedOrderProcessor() {
        this.orderPool = new ObjectPool<>(Order::new, 10000);
        this.memoryManager = new DirectMemoryManager(MEMORY_BUFFER_SIZE);
        this.orderCache = new OrderCache();
    }
    
    // Single order processing (optimized version)
    public ProcessingResult processOrder(OrderData orderData) {
        long startTime = System.nanoTime();
        
        Order tempOrder = orderPool.acquire(); // Temporary processing object
        try {
            // Data conversion using temporary order
            tempOrder.setId(orderData.getId());
            tempOrder.setSymbol(orderData.getSymbol());
            tempOrder.setPrice(orderData.getPrice());
            tempOrder.setQuantity(orderData.getQuantity());
            tempOrder.setSide(orderData.getSide());
            tempOrder.setTimestamp(startTime);
            
            // Business processing validation
            validateOrder(tempOrder);
            
            // Create a persistent copy for cache (fixes object lifecycle bug)
            Order persistentOrder = new Order(tempOrder);
            orderCache.addOrder(persistentOrder);
            
            // Serialize the temporary order to direct memory
            boolean serialized = memoryManager.serializeOrder(tempOrder);
            
            // Update statistics  
            processedCount.incrementAndGet();
            long processingTime = System.nanoTime() - startTime;
            totalProcessingTime.addAndGet(processingTime);
            
            return new ProcessingResult(true, processingTime, serialized);
            
        } catch (Exception e) {
            return new ProcessingResult(false, System.nanoTime() - startTime, false, e);
        } finally {
            // Safe to release the temporary object as cache has its own copy
            orderPool.release(tempOrder);
        }
    }
    
    // Batch order processing (high-performance version)
    public BatchProcessingResult processOrdersBatch(List<OrderData> orderDataList) {
        if (orderDataList.isEmpty()) {
            return new BatchProcessingResult(0, 0, 0, 0);
        }
        
        long startTime = System.nanoTime();
        int successCount = 0;
        int failureCount = 0;
        List<Order> tempOrderBatch = new ArrayList<>(BATCH_SIZE);
        
        try {
            // Batch acquire temporary order objects
            for (int i = 0; i < orderDataList.size(); i++) {
                Order tempOrder = orderPool.acquire();
                tempOrderBatch.add(tempOrder);
            }
            
            // Batch data conversion and processing
            for (int i = 0; i < orderDataList.size(); i++) {
                OrderData data = orderDataList.get(i);
                Order tempOrder = tempOrderBatch.get(i);
                
                try {
                    // Set data on temporary order
                    tempOrder.setId(data.getId());
                    tempOrder.setSymbol(data.getSymbol());
                    tempOrder.setPrice(data.getPrice());
                    tempOrder.setQuantity(data.getQuantity());
                    tempOrder.setSide(data.getSide());
                    tempOrder.setTimestamp(System.nanoTime());
                    
                    validateOrder(tempOrder);
                    
                    // Create persistent copy for cache (fixes object lifecycle bug)
                    Order persistentOrder = new Order(tempOrder);
                    orderCache.addOrder(persistentOrder);
                    successCount++;
                    
                } catch (Exception e) {
                    failureCount++;
                    // Log error but continue processing other orders
                }
            }
            
            // Batch serialization using temporary orders
            int serializedCount = memoryManager.serializeOrderBatch(tempOrderBatch);
            
            // Batch statistics update
            processedCount.addAndGet(successCount);
            long totalTime = System.nanoTime() - startTime;
            totalProcessingTime.addAndGet(totalTime);
            
            return new BatchProcessingResult(successCount, failureCount, 
                                           totalTime, serializedCount);
            
        } finally {
            // Safe to release temporary objects as cache has its own copies
            for (Order tempOrder : tempOrderBatch) {
                if (tempOrder != null) {
                    orderPool.release(tempOrder);
                }
            }
        }
    }
    
    // Order validation
    private void validateOrder(Order order) {
        if (order.getPrice() <= 0) {
            throw new IllegalArgumentException("Invalid price: " + order.getPrice());
        }
        if (order.getQuantity() <= 0) {
            throw new IllegalArgumentException("Invalid quantity: " + order.getQuantity());
        }
        if (order.getSymbol() == null || order.getSymbol().isEmpty()) {
            throw new IllegalArgumentException("Invalid symbol");
        }
    }
    
    // Performance statistics
    public PerformanceStats getPerformanceStats() {
        long count = processedCount.get();
        long totalTime = totalProcessingTime.get();
        
        double avgLatencyNs = count > 0 ? (double) totalTime / count : 0;
        double throughputPerSec = count > 0 ? count * 1_000_000_000.0 / totalTime : 0;
        
        return new PerformanceStats(count, avgLatencyNs, throughputPerSec, 
                                  orderCache.size(), orderPool.size());
    }
    
    // Resource cleanup
    public void shutdown() {
        orderCache.clear();
        memoryManager.reset();
        // Object pool will be cleaned up automatically
    }
    
    // Memory status check
    public MemoryStatus getMemoryStatus() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return new MemoryStatus(
            usedMemory,
            totalMemory,
            memoryManager.getUsedSize(),
            memoryManager.getRemainingSize()
        );
    }
    
    // Getters for components (for testing)
    public ObjectPool<Order> getOrderPool() {
        return orderPool;
    }
    
    public DirectMemoryManager getMemoryManager() {
        return memoryManager;
    }
    
    public OrderCache getOrderCache() {
        return orderCache;
    }
    
    // Get order by ID
    public Order getOrder(int orderId) {
        return orderCache.getOrder(orderId);
    }
    
    // Get orders by symbol
    public List<Order> getOrdersBySymbol(String symbol) {
        return orderCache.getOrdersBySymbol(symbol);
    }
    
    // Remove order
    public Order removeOrder(int orderId) {
        return orderCache.removeOrder(orderId);
    }
}