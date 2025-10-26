package com.hft.lockfree.handler;

import com.hft.lockfree.event.PriceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 价格聚合处理器
 * 
 * 职责：
 * 1. 聚合同一交易品种的价格数据
 * 2. 计算派生指标（价差、波动率等）
 * 3. 维护最新价格簿
 */
public class AggregationHandler implements PriceEventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AggregationHandler.class);
    
    // 价格簿：存储每个交易品种的最新价格
    private final ConcurrentHashMap<String, PriceSnapshot> priceBook = new ConcurrentHashMap<>();
    
    // 统计信息
    private final AtomicLong processedEvents = new AtomicLong(0);
    private final AtomicLong priceUpdates = new AtomicLong(0);
    
    @Override
    public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) throws Exception {
        // 只处理已验证的事件
        if (!event.isProcessed()) {
            return;
        }
        
        processedEvents.incrementAndGet();
        
        try {
            String symbol = event.getSymbol();
            
            // 更新价格簿
            updatePriceBook(event);
            
            // 计算派生指标
            calculateDerivedMetrics(event);
            
            priceUpdates.incrementAndGet();
            
        } catch (Exception e) {
            logger.error("Error processing aggregation for sequence {}: {}", sequence, e.getMessage(), e);
        }
    }
    
    /**
     * 更新价格簿
     */
    private void updatePriceBook(PriceEvent event) {
        String symbol = event.getSymbol();
        
        priceBook.compute(symbol, (key, existing) -> {
            if (existing == null) {
                // 新的交易品种
                return new PriceSnapshot(event);
            } else {
                // 更新现有价格
                existing.update(event);
                return existing;
            }
        });
    }
    
    /**
     * 计算派生指标
     */
    private void calculateDerivedMetrics(PriceEvent event) {
        PriceSnapshot snapshot = priceBook.get(event.getSymbol());
        if (snapshot != null) {
            // 更新价格变化统计
            snapshot.updateStatistics(event);
        }
    }
    
    /**
     * 获取指定交易品种的最新价格
     */
    public PriceSnapshot getLatestPrice(String symbol) {
        return priceBook.get(symbol);
    }
    
    /**
     * 获取所有交易品种
     */
    public java.util.Set<String> getAllSymbols() {
        return priceBook.keySet();
    }
    
    @Override
    public String getHandlerName() {
        return "AggregationHandler";
    }
    
    @Override
    public void initialize() {
        logger.info("AggregationHandler initialized");
        resetStatistics();
    }
    
    @Override
    public void shutdown() {
        logger.info("AggregationHandler shutdown. Final statistics: {}", getStatistics());
    }
    
    @Override
    public String getStatistics() {
        return String.format("AggregationHandler[processed=%d, updates=%d, symbols=%d]", 
                processedEvents.get(), priceUpdates.get(), priceBook.size());
    }
    
    @Override
    public void resetStatistics() {
        processedEvents.set(0);
        priceUpdates.set(0);
        priceBook.clear();
    }
    
    /**
     * 价格快照类 - 存储单个交易品种的最新价格和统计信息
     */
    public static class PriceSnapshot {
        private volatile String symbol;
        private volatile double bidPrice;
        private volatile double askPrice;
        private volatile long timestamp;
        private volatile long updateCount;
        
        // 统计信息
        private volatile double previousMidPrice;
        private volatile double priceChange;
        private volatile double volatility;
        
        public PriceSnapshot(PriceEvent event) {
            update(event);
            this.updateCount = 1;
            this.previousMidPrice = event.getMidPrice();
        }
        
        public void update(PriceEvent event) {
            this.symbol = event.getSymbol();
            this.bidPrice = event.getBidPrice();
            this.askPrice = event.getAskPrice();
            this.timestamp = event.getTimestamp();
        }
        
        public void updateStatistics(PriceEvent event) {
            double currentMidPrice = event.getMidPrice();
            this.priceChange = currentMidPrice - previousMidPrice;
            this.previousMidPrice = currentMidPrice;
            this.updateCount++;
            
            // 简单的波动率计算（移动平均）
            this.volatility = this.volatility * 0.95 + Math.abs(priceChange) * 0.05;
        }
        
        // Getter方法
        public String getSymbol() { return symbol; }
        public double getBidPrice() { return bidPrice; }
        public double getAskPrice() { return askPrice; }
        public long getTimestamp() { return timestamp; }
        public long getUpdateCount() { return updateCount; }
        public double getPriceChange() { return priceChange; }
        public double getVolatility() { return volatility; }
        
        public double getSpread() { return askPrice - bidPrice; }
        public double getMidPrice() { return (bidPrice + askPrice) / 2.0; }
        
        @Override
        public String toString() {
            return String.format("PriceSnapshot{symbol='%s', bid=%.5f, ask=%.5f, mid=%.5f, change=%.5f, updates=%d}", 
                    symbol, bidPrice, askPrice, getMidPrice(), priceChange, updateCount);
        }
    }
}