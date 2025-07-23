package com.hft.memory.core;

public class PerformanceStats {
    private final long totalProcessed;
    private final double avgLatencyNs;
    private final double throughputPerSec;
    private final int cacheSize;
    private final int poolSize;
    
    public PerformanceStats(long totalProcessed, double avgLatencyNs, 
                          double throughputPerSec, int cacheSize, int poolSize) {
        this.totalProcessed = totalProcessed;
        this.avgLatencyNs = avgLatencyNs;
        this.throughputPerSec = throughputPerSec;
        this.cacheSize = cacheSize;
        this.poolSize = poolSize;
    }
    
    @Override
    public String toString() {
        return String.format(
            "PerformanceStats{processed=%d, avgLatency=%.2fμs, " +
            "throughput=%.0f/sec, cache=%d, pool=%d}",
            totalProcessed, avgLatencyNs / 1000, throughputPerSec, cacheSize, poolSize
        );
    }
    
    // Getters
    public long getTotalProcessed() { return totalProcessed; }
    public double getAvgLatencyNs() { return avgLatencyNs; }
    public double getThroughputPerSec() { return throughputPerSec; }
    public int getCacheSize() { return cacheSize; }
    public int getPoolSize() { return poolSize; }
}