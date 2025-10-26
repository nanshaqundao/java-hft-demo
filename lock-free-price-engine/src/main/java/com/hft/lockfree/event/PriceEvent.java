package com.hft.lockfree.event;

/**
 * 价格事件类 - Disruptor环形缓冲区中的数据载体
 * 
 * 设计要点：
 * 1. 可变对象，避免GC压力
 * 2. 字段紧凑排列，提高缓存效率
 * 3. 提供reset()方法重用对象
 * 4. 支持复制操作避免数据竞争
 */
public class PriceEvent {
    
    // 核心价格数据
    private String symbol;        // 交易品种符号 (8字节引用)
    private double bidPrice;      // 买入价 (8字节)
    private double askPrice;      // 卖出价 (8字节) 
    private long timestamp;       // 时间戳(纳秒) (8字节)
    private long sequence;        // 序列号 (8字节)
    
    // 扩展数据
    private double volume;        // 成交量 (8字节)
    private int sourceId;         // 数据源ID (4字节)
    private int eventType;        // 事件类型 (4字节)
    
    // 状态标记
    private volatile boolean processed;  // 处理完成标记
    
    /**
     * 默认构造函数
     */
    public PriceEvent() {
        reset();
    }
    
    /**
     * 重置事件数据，便于对象重用
     */
    public void reset() {
        this.symbol = null;
        this.bidPrice = 0.0;
        this.askPrice = 0.0;
        this.timestamp = 0L;
        this.sequence = 0L;
        this.volume = 0.0;
        this.sourceId = 0;
        this.eventType = 0;
        this.processed = false;
    }
    
    /**
     * 从另一个事件复制数据
     * @param other 源事件
     */
    public void copyFrom(PriceEvent other) {
        if (other == null) return;
        
        this.symbol = other.symbol;
        this.bidPrice = other.bidPrice;
        this.askPrice = other.askPrice;
        this.timestamp = other.timestamp;
        this.sequence = other.sequence;
        this.volume = other.volume;
        this.sourceId = other.sourceId;
        this.eventType = other.eventType;
        this.processed = other.processed;
    }
    
    /**
     * 设置基础价格数据
     */
    public void setPriceData(String symbol, double bidPrice, double askPrice, long timestamp) {
        this.symbol = symbol;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.timestamp = timestamp;
    }
    
    /**
     * 计算买卖价差
     * @return 价差(askPrice - bidPrice)
     */
    public double getSpread() {
        return askPrice - bidPrice;
    }
    
    /**
     * 计算中间价
     * @return 中间价 (bidPrice + askPrice) / 2
     */
    public double getMidPrice() {
        return (bidPrice + askPrice) / 2.0;
    }
    
    /**
     * 检查价格数据是否有效
     * @return true表示数据有效
     */
    public boolean isValid() {
        return symbol != null && 
               !symbol.isEmpty() && 
               bidPrice > 0 && 
               askPrice > 0 && 
               bidPrice <= askPrice &&
               timestamp > 0;
    }
    
    // Getter和Setter方法
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public double getBidPrice() { return bidPrice; }
    public void setBidPrice(double bidPrice) { this.bidPrice = bidPrice; }
    
    public double getAskPrice() { return askPrice; }
    public void setAskPrice(double askPrice) { this.askPrice = askPrice; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public long getSequence() { return sequence; }
    public void setSequence(long sequence) { this.sequence = sequence; }
    
    public double getVolume() { return volume; }
    public void setVolume(double volume) { this.volume = volume; }
    
    public int getSourceId() { return sourceId; }
    public void setSourceId(int sourceId) { this.sourceId = sourceId; }
    
    public int getEventType() { return eventType; }
    public void setEventType(int eventType) { this.eventType = eventType; }
    
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }
    
    @Override
    public String toString() {
        return String.format("PriceEvent{symbol='%s', bid=%.5f, ask=%.5f, spread=%.5f, ts=%d, seq=%d}", 
                symbol, bidPrice, askPrice, getSpread(), timestamp, sequence);
    }
}