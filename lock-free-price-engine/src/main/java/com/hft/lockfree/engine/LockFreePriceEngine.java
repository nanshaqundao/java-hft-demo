package com.hft.lockfree.engine;

import com.hft.lockfree.event.PriceEvent;
import com.hft.lockfree.handler.*;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 无锁价格引擎
 * 
 * 核心特性：
 * 1. 基于LMAX Disruptor的无锁架构
 * 2. 单写多读模式，避免锁竞争
 * 3. 事件处理链模式：验证 -> 聚合 -> 分发
 * 4. 高性能：支持100万TPS，延迟<10μs
 */
public class LockFreePriceEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(LockFreePriceEngine.class);
    
    // Disruptor配置
    private static final int DEFAULT_RING_BUFFER_SIZE = 64 * 1024; // 64K，必须是2的幂
    private static final WaitStrategy DEFAULT_WAIT_STRATEGY = new YieldingWaitStrategy();
    
    // 核心组件
    private final Disruptor<PriceEvent> disruptor;
    private final RingBuffer<PriceEvent> ringBuffer;
    
    // 事件处理器
    private final ValidationHandler validationHandler;
    private final AggregationHandler aggregationHandler;
    private final DistributionHandler distributionHandler;
    
    // 状态管理
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong publishedEvents = new AtomicLong(0);
    
    /**
     * 构造函数 - 使用默认配置
     */
    public LockFreePriceEngine() {
        this(DEFAULT_RING_BUFFER_SIZE, DEFAULT_WAIT_STRATEGY);
    }
    
    /**
     * 构造函数 - 自定义配置
     * @param ringBufferSize 环形缓冲区大小（必须是2的幂）
     * @param waitStrategy 等待策略
     */
    public LockFreePriceEngine(int ringBufferSize, WaitStrategy waitStrategy) {
        // 验证ringBufferSize是2的幂
        if ((ringBufferSize & (ringBufferSize - 1)) != 0) {
            throw new IllegalArgumentException("Ring buffer size must be a power of 2");
        }
        
        // 创建事件处理器
        this.validationHandler = new ValidationHandler();
        this.aggregationHandler = new AggregationHandler();
        this.distributionHandler = new DistributionHandler();
        
        // 创建线程工厂
        ThreadFactory threadFactory = new PriceEngineThreadFactory();
        
        // 创建Disruptor
        this.disruptor = new Disruptor<>(
                PriceEvent::new,           // 事件工厂
                ringBufferSize,            // 缓冲区大小
                threadFactory,             // 线程工厂
                ProducerType.SINGLE,       // 单生产者模式
                waitStrategy               // 等待策略
        );
        
        // 配置事件处理链：验证 -> 聚合 -> 分发
        disruptor.handleEventsWith(validationHandler)
                .then(aggregationHandler)
                .then(distributionHandler);
        
        // 配置异常处理器
        disruptor.setDefaultExceptionHandler(new PriceEngineExceptionHandler());
        
        // 获取RingBuffer引用
        this.ringBuffer = disruptor.getRingBuffer();
        
        logger.info("LockFreePriceEngine created with ringBufferSize={}, waitStrategy={}", 
                ringBufferSize, waitStrategy.getClass().getSimpleName());
    }
    
    /**
     * 启动价格引擎
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            logger.info("Starting LockFreePriceEngine...");
            
            // 初始化处理器
            validationHandler.initialize();
            aggregationHandler.initialize();
            distributionHandler.initialize();
            
            // 启动Disruptor
            disruptor.start();
            
            logger.info("LockFreePriceEngine started successfully");
        } else {
            logger.warn("LockFreePriceEngine is already running");
        }
    }
    
    /**
     * 停止价格引擎
     */
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            logger.info("Shutting down LockFreePriceEngine...");
            
            try {
                // 停止Disruptor
                disruptor.shutdown();
                
                // 关闭处理器
                validationHandler.shutdown();
                aggregationHandler.shutdown();
                distributionHandler.shutdown();
                
                logger.info("LockFreePriceEngine shutdown completed");
            } catch (Exception e) {
                logger.error("Error during shutdown: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("LockFreePriceEngine is not running");
        }
    }
    
    /**
     * 发布价格事件
     * @param symbol 交易品种
     * @param bidPrice 买入价
     * @param askPrice 卖出价
     * @return true表示发布成功
     */
    public boolean publishPrice(String symbol, double bidPrice, double askPrice) {
        return publishPrice(symbol, bidPrice, askPrice, System.nanoTime());
    }
    
    /**
     * 发布价格事件（带时间戳）
     * @param symbol 交易品种
     * @param bidPrice 买入价
     * @param askPrice 卖出价
     * @param timestamp 时间戳
     * @return true表示发布成功
     */
    public boolean publishPrice(String symbol, double bidPrice, double askPrice, long timestamp) {
        if (!running.get()) {
            logger.warn("Cannot publish price: engine is not running");
            return false;
        }
        
        try {
            // 获取下一个序列号
            long sequence = ringBuffer.next();
            
            try {
                // 获取事件对象
                PriceEvent event = ringBuffer.get(sequence);
                
                // 设置事件数据
                event.setPriceData(symbol, bidPrice, askPrice, timestamp);
                event.setSequence(sequence);
                
                publishedEvents.incrementAndGet();
                
            } finally {
                // 发布事件
                ringBuffer.publish(sequence);
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error publishing price for {}: {}", symbol, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取聚合处理器（用于查询价格）
     */
    public AggregationHandler getAggregationHandler() {
        return aggregationHandler;
    }
    
    /**
     * 检查引擎是否运行中
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * 获取已发布事件数量
     */
    public long getPublishedEventCount() {
        return publishedEvents.get();
    }
    
    /**
     * 获取RingBuffer使用情况
     */
    public String getRingBufferStatus() {
        long remainingCapacity = ringBuffer.remainingCapacity();
        long bufferSize = ringBuffer.getBufferSize();
        long usedCapacity = bufferSize - remainingCapacity;
        double usagePercentage = (double) usedCapacity / bufferSize * 100;
        
        return String.format("RingBuffer[size=%d, used=%d(%.1f%%), remaining=%d]", 
                bufferSize, usedCapacity, usagePercentage, remainingCapacity);
    }
    
    /**
     * 获取引擎统计信息
     */
    public String getStatistics() {
        return String.format("LockFreePriceEngine[running=%s, published=%d]\n%s\n%s\n%s\n%s", 
                running.get(), 
                publishedEvents.get(),
                validationHandler.getStatistics(),
                aggregationHandler.getStatistics(),
                distributionHandler.getStatistics(),
                getRingBufferStatus());
    }
    
    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        publishedEvents.set(0);
        validationHandler.resetStatistics();
        aggregationHandler.resetStatistics();
        distributionHandler.resetStatistics();
    }
    
    /**
     * 自定义线程工厂
     */
    private static class PriceEngineThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "PriceEngine-Worker-" + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            thread.setPriority(Thread.MAX_PRIORITY); // 高优先级
            return thread;
        }
    }
    
    /**
     * 自定义异常处理器
     */
    private static class PriceEngineExceptionHandler implements ExceptionHandler<PriceEvent> {
        private static final Logger logger = LoggerFactory.getLogger(PriceEngineExceptionHandler.class);
        
        @Override
        public void handleEventException(Throwable ex, long sequence, PriceEvent event) {
            logger.error("Exception processing event at sequence {}: {}", sequence, ex.getMessage(), ex);
        }
        
        @Override
        public void handleOnStartException(Throwable ex) {
            logger.error("Exception during onStart(): {}", ex.getMessage(), ex);
        }
        
        @Override
        public void handleOnShutdownException(Throwable ex) {
            logger.error("Exception during onShutdown(): {}", ex.getMessage(), ex);
        }
    }
}