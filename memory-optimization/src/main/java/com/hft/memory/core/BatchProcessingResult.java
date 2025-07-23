package com.hft.memory.core;

public class BatchProcessingResult {
    private final int successCount;
    private final int failureCount;
    private final long totalTimeNs;
    private final int serializedCount;
    
    public BatchProcessingResult(int successCount, int failureCount, 
                               long totalTimeNs, int serializedCount) {
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.totalTimeNs = totalTimeNs;
        this.serializedCount = serializedCount;
    }
    
    // Getters and calculations
    public int getSuccessCount() { return successCount; }
    public int getFailureCount() { return failureCount; }
    public long getTotalTimeNs() { return totalTimeNs; }
    public int getSerializedCount() { return serializedCount; }
    
    public double getAvgLatencyNs() {
        return successCount > 0 ? (double) totalTimeNs / successCount : 0;
    }
    
    public double getThroughputPerSec() {
        return totalTimeNs > 0 ? successCount * 1_000_000_000.0 / totalTimeNs : 0;
    }
    
    @Override
    public String toString() {
        return String.format("BatchResult{success=%d, failure=%d, avgLatency=%.2fμs, throughput=%.0f/sec}",
            successCount, failureCount, getAvgLatencyNs() / 1000.0, getThroughputPerSec());
    }
}