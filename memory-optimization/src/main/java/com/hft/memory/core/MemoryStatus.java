package com.hft.memory.core;

public class MemoryStatus {
    private final long heapUsed;
    private final long heapTotal;
    private final int directUsed;
    private final int directRemaining;
    
    public MemoryStatus(long heapUsed, long heapTotal, 
                       int directUsed, int directRemaining) {
        this.heapUsed = heapUsed;
        this.heapTotal = heapTotal;
        this.directUsed = directUsed;
        this.directRemaining = directRemaining;
    }
    
    @Override
    public String toString() {
        return String.format(
            "Memory{heap=%.1fMB/%.1fMB, direct=%.1fMB/%.1fMB}",
            heapUsed / 1024.0 / 1024.0,
            heapTotal / 1024.0 / 1024.0,
            directUsed / 1024.0 / 1024.0,
            (directUsed + directRemaining) / 1024.0 / 1024.0
        );
    }
    
    // Getters
    public long getHeapUsed() { return heapUsed; }
    public long getHeapTotal() { return heapTotal; }  
    public int getDirectUsed() { return directUsed; }
    public int getDirectRemaining() { return directRemaining; }
}