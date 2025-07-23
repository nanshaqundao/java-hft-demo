package com.hft.memory.benchmark;

import com.hft.memory.core.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TraditionalOrderProcessor {
    private final Map<Integer, Order> orderCache = new ConcurrentHashMap<>();
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    
    public ProcessingResult processOrder(OrderData orderData) {
        long startTime = System.nanoTime();
        
        // Create new object every time
        Order order = new Order();
        order.setId(orderData.getId());
        order.setSymbol(orderData.getSymbol());
        order.setPrice(orderData.getPrice());
        order.setQuantity(orderData.getQuantity());
        order.setSide(orderData.getSide());
        order.setTimestamp(startTime);
        
        // Use Java Collections (with boxing overhead)
        orderCache.put(order.getId(), order);
        
        processedCount.incrementAndGet();
        long processingTime = System.nanoTime() - startTime;
        totalProcessingTime.addAndGet(processingTime);
        
        return new ProcessingResult(true, processingTime, false);
    }
    
    public BatchProcessingResult processOrdersBatch(List<OrderData> orderDataList) {
        long startTime = System.nanoTime();
        int successCount = 0;
        
        for (OrderData data : orderDataList) {
            ProcessingResult result = processOrder(data);
            if (result.isSuccess()) {
                successCount++;
            }
        }
        
        long totalTime = System.nanoTime() - startTime;
        return new BatchProcessingResult(successCount, 0, totalTime, 0);
    }
    
    public Order getOrder(int orderId) {
        return orderCache.get(orderId);
    }
    
    public PerformanceStats getPerformanceStats() {
        long count = processedCount.get();
        long totalTime = totalProcessingTime.get();
        
        double avgLatencyNs = count > 0 ? (double) totalTime / count : 0;
        double throughputPerSec = count > 0 ? count * 1_000_000_000.0 / totalTime : 0;
        
        return new PerformanceStats(count, avgLatencyNs, throughputPerSec, 
                                  orderCache.size(), 0);
    }
    
    public void shutdown() {
        orderCache.clear();
    }
}