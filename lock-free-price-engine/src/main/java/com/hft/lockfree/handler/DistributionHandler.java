package com.hft.lockfree.handler;

import com.hft.lockfree.event.PriceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 价格分发处理器
 * 
 * 职责：
 * 1. 将处理完成的价格事件分发给订阅者
 * 2. 记录处理日志
 * 3. 统计分发性能
 */
public class DistributionHandler implements PriceEventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(DistributionHandler.class);
    
    // 统计信息
    private final AtomicLong distributedEvents = new AtomicLong(0);
    private final AtomicLong totalLatency = new AtomicLong(0);
    private volatile long maxLatency = 0;
    private volatile long minLatency = Long.MAX_VALUE;
    
    @Override
    public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) throws Exception {
        // 只分发已处理的事件
        if (!event.isProcessed()) {
            return;
        }
        
        long startTime = System.nanoTime();
        
        try {
            // 分发到订阅者
            distributeToSubscribers(event);
            
            // 记录处理日志（可选，用于调试）
            if (logger.isDebugEnabled()) {
                logPriceUpdate(event, sequence);
            }
            
            // 计算延迟统计
            long latency = System.nanoTime() - startTime;
            updateLatencyStatistics(latency);
            
            distributedEvents.incrementAndGet();
            
        } catch (Exception e) {
            logger.error("Error distributing event at sequence {}: {}", sequence, e.getMessage(), e);
        }
    }
    
    /**
     * 分发价格事件到订阅者
     * 注意：这里只是模拟实现，实际应用中会连接到实际的消息系统
     */
    private void distributeToSubscribers(PriceEvent event) {
        // 模拟分发延迟
        // 在实际应用中，这里会：
        // 1. 发送到消息队列（如Kafka）
        // 2. 通知WebSocket客户端
        // 3. 更新数据库
        // 4. 触发算法交易逻辑
        
        // 模拟网络延迟
        try {
            Thread.sleep(0, 100); // 100纳秒模拟延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 记录价格更新日志
     */
    private void logPriceUpdate(PriceEvent event, long sequence) {
        long processingLatency = System.nanoTime() - event.getTimestamp();
        
        logger.debug("Price distributed: seq={}, symbol={}, bid={}, ask={}, latency={}ns", 
                sequence, event.getSymbol(), event.getBidPrice(), event.getAskPrice(), processingLatency);
    }
    
    /**
     * 更新延迟统计
     */
    private void updateLatencyStatistics(long latency) {
        totalLatency.addAndGet(latency);
        
        // 更新最大延迟
        if (latency > maxLatency) {
            maxLatency = latency;
        }
        
        // 更新最小延迟
        if (latency < minLatency) {
            minLatency = latency;
        }
    }
    
    @Override
    public String getHandlerName() {
        return "DistributionHandler";
    }
    
    @Override
    public void initialize() {
        logger.info("DistributionHandler initialized");
        resetStatistics();
    }
    
    @Override
    public void shutdown() {
        logger.info("DistributionHandler shutdown. Final statistics: {}", getStatistics());
    }
    
    @Override
    public String getStatistics() {
        long events = distributedEvents.get();
        long avgLatency = events > 0 ? totalLatency.get() / events : 0;
        
        return String.format("DistributionHandler[distributed=%d, avgLatency=%dns, minLatency=%dns, maxLatency=%dns]", 
                events, avgLatency, minLatency == Long.MAX_VALUE ? 0 : minLatency, maxLatency);
    }
    
    @Override
    public void resetStatistics() {
        distributedEvents.set(0);
        totalLatency.set(0);
        maxLatency = 0;
        minLatency = Long.MAX_VALUE;
    }
    
    /**
     * 获取平均分发延迟（纳秒）
     */
    public long getAverageLatency() {
        long events = distributedEvents.get();
        return events > 0 ? totalLatency.get() / events : 0;
    }
    
    /**
     * 获取分发事件总数
     */
    public long getDistributedEventCount() {
        return distributedEvents.get();
    }
    
    /**
     * 获取最大延迟（纳秒）
     */
    public long getMaxLatency() {
        return maxLatency;
    }
    
    /**
     * 获取最小延迟（纳秒）
     */
    public long getMinLatency() {
        return minLatency == Long.MAX_VALUE ? 0 : minLatency;
    }
}