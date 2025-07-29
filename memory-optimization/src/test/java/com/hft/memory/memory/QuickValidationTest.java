package com.hft.memory.memory;

import com.hft.memory.core.Order;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 快速验证所有策略的基本功能
 */
public class QuickValidationTest {
    
    private static final int BUFFER_SIZE = 1024;
    
    @Test
    void testAllStrategiesBasicFunction() {
        System.out.println("🚀 快速验证所有策略基本功能...");
        
        DirectMemoryStrategy[] strategies = {
            new SynchronizedDirectMemory(BUFFER_SIZE),
            new CASDirectMemory(BUFFER_SIZE),
            new ReadWriteLockDirectMemory(BUFFER_SIZE),
            new SegmentedLockDirectMemory(BUFFER_SIZE)
        };
        
        for (DirectMemoryStrategy strategy : strategies) {
            System.out.printf("  测试 %s...%n", strategy.getStrategyName());
            
            // 创建测试订单
            Order order = new Order();
            order.setId(42);
            order.setSymbol("TEST");
            order.setPriceAndQuantity(1.42, 4200);
            order.setSide((byte) 1);
            order.setTimestamp(System.nanoTime());
            
            // 测试写入
            assertTrue(strategy.serializeOrder(order), 
                      strategy.getStrategyName() + " 写入应该成功");
            
            // 测试读取
            Order result = strategy.deserializeOrderSafe(0);
            assertNotNull(result, strategy.getStrategyName() + " 读取应该成功");
            assertEquals(42, result.getId());
            assertEquals("TEST", result.getSymbol());
            assertEquals(1.42, result.getPrice(), 0.0001);
            assertEquals(4200, result.getQuantity());
            
            // 测试内存状态
            assertTrue(strategy.getUsedSize() > 0);
            assertTrue(strategy.getRemainingSize() < BUFFER_SIZE);
            
            System.out.printf("    ✅ %s 功能正常%n", strategy.getStrategyName());
        }
        
        System.out.println("🎉 所有策略验证通过！");
    }
}