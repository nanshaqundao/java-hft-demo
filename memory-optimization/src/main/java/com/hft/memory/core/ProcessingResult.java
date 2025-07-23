package com.hft.memory.core;

public class ProcessingResult {
    private final boolean success;
    private final long processingTimeNs;
    private final boolean serialized;
    private final Exception error;
    
    public ProcessingResult(boolean success, long processingTimeNs, boolean serialized) {
        this(success, processingTimeNs, serialized, null);
    }
    
    public ProcessingResult(boolean success, long processingTimeNs, 
                          boolean serialized, Exception error) {
        this.success = success;
        this.processingTimeNs = processingTimeNs;
        this.serialized = serialized;
        this.error = error;
    }
    
    public boolean isSuccess() { return success; }
    public long getProcessingTimeNs() { return processingTimeNs; }
    public boolean isSerialized() { return serialized; }
    public Exception getError() { return error; }
    
    @Override
    public String toString() {
        return String.format("ProcessingResult{success=%s, timeNs=%d, serialized=%s}",
            success, processingTimeNs, serialized);
    }
}