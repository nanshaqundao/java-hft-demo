package com.hft.lockfree.handler;

import com.hft.lockfree.event.PriceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 价格验证处理器
 * 
 * 职责：
 * 1. 验证价格数据的有效性
 * 2. 检查数据范围和逻辑一致性
 * 3. 统计验证结果
 */
public class ValidationHandler implements PriceEventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationHandler.class);
    
    // 验证配置
    private static final double MIN_PRICE = 0.0001;  // 最小价格
    private static final double MAX_PRICE = 1000000; // 最大价格
    private static final double MAX_SPREAD_RATIO = 0.1; // 最大价差比例
    
    // 统计信息
    private final AtomicLong totalEvents = new AtomicLong(0);
    private final AtomicLong validEvents = new AtomicLong(0);
    private final AtomicLong invalidEvents = new AtomicLong(0);
    
    @Override
    public void onEvent(PriceEvent event, long sequence, boolean endOfBatch) throws Exception {
        totalEvents.incrementAndGet();
        
        try {
            if (validateEvent(event)) {
                validEvents.incrementAndGet();
                // 标记为已处理
                event.setProcessed(true);
            } else {
                invalidEvents.incrementAndGet();
                // 无效数据，重置事件
                event.reset();
                logger.warn("Invalid price event at sequence {}: {}", sequence, event);
            }
        } catch (Exception e) {
            invalidEvents.incrementAndGet();
            logger.error("Error validating event at sequence {}: {}", sequence, e.getMessage(), e);
            // 发生异常时重置事件
            event.reset();
        }
    }
    
    /**
     * 验证价格事件
     * @param event 待验证的事件
     * @return true表示有效
     */
    private boolean validateEvent(PriceEvent event) {
        // 基础数据检查
        if (!event.isValid()) {
            return false;
        }
        
        // 价格范围检查
        if (event.getBidPrice() < MIN_PRICE || event.getBidPrice() > MAX_PRICE ||
            event.getAskPrice() < MIN_PRICE || event.getAskPrice() > MAX_PRICE) {
            return false;
        }
        
        // 价差合理性检查
        double spread = event.getSpread();
        double midPrice = event.getMidPrice();
        if (spread / midPrice > MAX_SPREAD_RATIO) {
            return false;
        }
        
        // 时间戳检查（不能是未来时间）
        long currentTime = System.nanoTime();
        if (event.getTimestamp() > currentTime) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getHandlerName() {
        return "ValidationHandler";
    }
    
    @Override
    public void initialize() {
        logger.info("ValidationHandler initialized");
        resetStatistics();
    }
    
    @Override
    public void shutdown() {
        logger.info("ValidationHandler shutdown. Final statistics: {}", getStatistics());
    }
    
    @Override
    public String getStatistics() {
        long total = totalEvents.get();
        long valid = validEvents.get();
        long invalid = invalidEvents.get();
        double validRate = total > 0 ? (double) valid / total * 100 : 0;
        
        return String.format("ValidationHandler[total=%d, valid=%d(%.2f%%), invalid=%d]", 
                total, valid, validRate, invalid);
    }
    
    @Override
    public void resetStatistics() {
        totalEvents.set(0);
        validEvents.set(0);
        invalidEvents.set(0);
    }
    
    /**
     * 获取验证通过率
     * @return 验证通过率(0-1)
     */
    public double getValidationRate() {
        long total = totalEvents.get();
        return total > 0 ? (double) validEvents.get() / total : 0.0;
    }
}