package com.hft.lockfree.handler;

import com.hft.lockfree.event.PriceEvent;
import com.lmax.disruptor.EventHandler;

/**
 * 价格事件处理器基础接口
 * 
 * 继承自Disruptor的EventHandler，提供额外的生命周期管理
 * 和性能监控接口
 */
public interface PriceEventHandler extends EventHandler<PriceEvent> {
    
    /**
     * 处理器名称，用于监控和日志
     * @return 处理器名称
     */
    String getHandlerName();
    
    /**
     * 初始化处理器
     * 在处理器开始工作前调用
     */
    default void initialize() {
        // 默认空实现
    }
    
    /**
     * 关闭处理器
     * 在处理器停止工作后调用，用于资源清理
     */
    default void shutdown() {
        // 默认空实现
    }
    
    /**
     * 获取处理器统计信息
     * @return 统计信息字符串
     */
    default String getStatistics() {
        return String.format("Handler[%s]: No statistics available", getHandlerName());
    }
    
    /**
     * 重置统计信息
     */
    default void resetStatistics() {
        // 默认空实现
    }
}