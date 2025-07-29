package com.hft.memory.memory;

import com.hft.memory.core.Order;
import java.util.List;

/**
 * 直接内存管理策略接口
 * 统一4种不同并发策略的API，便于性能对比测试
 * 
 * 实现策略：
 * 1. SynchronizedDirectMemory - 传统synchronized方式
 * 2. CASDirectMemory - 纯CAS无锁实现
 * 3. ReadWriteLockDirectMemory - 读写锁分离
 * 4. SegmentedLockDirectMemory - 分段锁实现
 */
public interface DirectMemoryStrategy {
    
    /**
     * 序列化订单到直接内存
     * @param order 要序列化的订单
     * @return 成功返回true，失败返回false
     */
    boolean serializeOrder(Order order);
    
    /**
     * 从直接内存反序列化订单
     * @param reusableOrder 可重用的订单对象
     * @param offset 在缓冲区中的偏移量
     * @return 反序列化的订单，失败返回null
     */
    Order deserializeOrder(Order reusableOrder, int offset);
    
    /**
     * 线程安全的反序列化方法，创建新的Order对象
     * @param offset 在缓冲区中的偏移量
     * @return 新的订单对象，失败返回null
     */
    Order deserializeOrderSafe(int offset);
    
    /**
     * 批量序列化订单
     * @param orders 订单列表
     * @return 成功序列化的订单数量
     */
    int serializeOrderBatch(List<Order> orders);
    
    /**
     * 环形缓冲区方式序列化（HFT优化）
     * @param order 要序列化的订单
     * @return 成功返回true，失败返回false
     */
    boolean serializeOrderRing(Order order);
    
    /**
     * 重置缓冲区
     */
    void reset();
    
    /**
     * 强制重置（HFT优化，不清理内容）
     */
    void forceReset();
    
    /**
     * 获取已使用的字节数
     * @return 已使用的字节数
     */
    int getUsedSize();
    
    /**
     * 获取剩余空间
     * @return 剩余字节数
     */
    int getRemainingSize();
    
    /**
     * 获取缓冲区总大小
     * @return 总字节数
     */
    int getBufferSize();
    
    /**
     * 检查是否接近满载
     * @param threshold 阈值(0.0-1.0)
     * @return 超过阈值返回true
     */
    boolean isAlmostFull(double threshold);
    
    /**
     * 获取剩余可存储的订单数量
     * @return 剩余订单容量
     */
    int getRemainingOrderCapacity();
    
    /**
     * 获取策略名称（用于性能测试标识）
     * @return 策略名称
     */
    String getStrategyName();
    
    /**
     * 获取策略描述（用于性能测试报告）
     * @return 策略的详细描述
     */
    String getStrategyDescription();
}