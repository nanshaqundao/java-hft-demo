package com.hft.memory.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the fix for the object lifecycle bug where cached orders
 * were getting corrupted when pooled objects were reset.
 */
class ObjectLifecycleBugTest {
    
    private MemoryOptimizedOrderProcessor processor;
    
    @BeforeEach
    void setUp() {
        processor = new MemoryOptimizedOrderProcessor();
    }
    
    @Test
    void testCachedOrderIntegrityAfterPoolReuse() {
        // Create two different order data
        OrderData orderData1 = new OrderData(1, "EURUSD", 1.1234, 1000, (byte)1);
        OrderData orderData2 = new OrderData(2, "GBPUSD", 1.2345, 2000, (byte)2);
        
        // Process first order
        ProcessingResult result1 = processor.processOrder(orderData1);
        assertTrue(result1.isSuccess());
        
        // Verify first order is in cache with correct data
        Order cachedOrder1 = processor.getOrder(1);
        assertNotNull(cachedOrder1);
        assertEquals(1, cachedOrder1.getId());
        assertEquals("EURUSD", cachedOrder1.getSymbol());
        assertEquals(1.1234, cachedOrder1.getPrice(), 0.0001);
        assertEquals(1000, cachedOrder1.getQuantity());
        assertEquals((byte)1, cachedOrder1.getSide());
        
        // Process second order (may reuse same pooled object)
        ProcessingResult result2 = processor.processOrder(orderData2);
        assertTrue(result2.isSuccess());
        
        // Verify second order is in cache with correct data
        Order cachedOrder2 = processor.getOrder(2);
        assertNotNull(cachedOrder2);
        assertEquals(2, cachedOrder2.getId());
        assertEquals("GBPUSD", cachedOrder2.getSymbol());
        assertEquals(1.2345, cachedOrder2.getPrice(), 0.0001);
        assertEquals(2000, cachedOrder2.getQuantity());
        assertEquals((byte)2, cachedOrder2.getSide());
        
        // CRITICAL TEST: Verify first order's cached data is still intact
        // (This would fail before the fix due to object pool reuse)
        Order stillCachedOrder1 = processor.getOrder(1);
        assertNotNull(stillCachedOrder1);
        assertEquals(1, stillCachedOrder1.getId());
        assertEquals("EURUSD", stillCachedOrder1.getSymbol());
        assertEquals(1.1234, stillCachedOrder1.getPrice(), 0.0001);
        assertEquals(1000, stillCachedOrder1.getQuantity());
        assertEquals((byte)1, stillCachedOrder1.getSide());
        
        // Verify cached orders are different object instances
        assertNotSame(cachedOrder1, cachedOrder2, 
            "Cached orders should be different object instances");
        
        processor.shutdown();
    }
    
    @Test
    void testBatchProcessingCacheIntegrity() {
        // Create multiple orders
        OrderData[] orderDataArray = {
            new OrderData(10, "EURUSD", 1.1100, 1000, (byte)1),
            new OrderData(11, "GBPUSD", 1.2200, 2000, (byte)2),
            new OrderData(12, "USDJPY", 110.50, 3000, (byte)1)
        };
        
        java.util.List<OrderData> orderList = java.util.Arrays.asList(orderDataArray);
        
        // Process batch
        BatchProcessingResult batchResult = processor.processOrdersBatch(orderList);
        assertEquals(3, batchResult.getSuccessCount());
        assertEquals(0, batchResult.getFailureCount());
        
        // Verify all orders are cached with correct data
        for (OrderData originalData : orderDataArray) {
            Order cachedOrder = processor.getOrder(originalData.getId());
            assertNotNull(cachedOrder, "Order " + originalData.getId() + " should be cached");
            assertEquals(originalData.getId(), cachedOrder.getId());
            assertEquals(originalData.getSymbol(), cachedOrder.getSymbol());
            assertEquals(originalData.getPrice(), cachedOrder.getPrice(), 0.0001);
            assertEquals(originalData.getQuantity(), cachedOrder.getQuantity());
            assertEquals(originalData.getSide(), cachedOrder.getSide());
        }
        
        // Process more orders to potentially reuse pooled objects
        OrderData[] moreOrderData = {
            new OrderData(20, "AUDUSD", 0.7500, 4000, (byte)2),
            new OrderData(21, "USDCAD", 1.3500, 5000, (byte)1)
        };
        
        java.util.List<OrderData> moreOrderList = java.util.Arrays.asList(moreOrderData);
        BatchProcessingResult moreBatchResult = processor.processOrdersBatch(moreOrderList);
        assertEquals(2, moreBatchResult.getSuccessCount());
        
        // Verify original cached orders are still intact
        for (OrderData originalData : orderDataArray) {
            Order stillCachedOrder = processor.getOrder(originalData.getId());
            assertNotNull(stillCachedOrder, "Original order " + originalData.getId() + " should still be cached");
            assertEquals(originalData.getId(), stillCachedOrder.getId());
            assertEquals(originalData.getSymbol(), stillCachedOrder.getSymbol());
            assertEquals(originalData.getPrice(), stillCachedOrder.getPrice(), 0.0001);
            assertEquals(originalData.getQuantity(), stillCachedOrder.getQuantity());
            assertEquals(originalData.getSide(), stillCachedOrder.getSide());
        }
        
        processor.shutdown();
    }
    
    @Test
    void testOrderCopyConstructor() {
        // Test the new copy constructor
        Order original = new Order(1, "EURUSD", 1.1234, 1000);
        Order copy = new Order(original);
        
        // Verify data is copied correctly
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getSymbol(), copy.getSymbol());
        assertEquals(original.getPrice(), copy.getPrice(), 0.0001);
        assertEquals(original.getQuantity(), copy.getQuantity());
        assertEquals(original.getSide(), copy.getSide());
        assertEquals(original.getType(), copy.getType());
        
        // Verify they are different object instances
        assertNotSame(original, copy);
        
        // Verify changes to original don't affect copy
        original.setPrice(2.0000);
        assertNotEquals(original.getPrice(), copy.getPrice());
    }
    
    @Test
    void testOrderCopyFromMethod() {
        // Test the copyFrom method
        Order source = new Order(1, "GBPUSD", 1.2345, 2000);
        Order target = new Order();
        
        target.copyFrom(source);
        
        // Verify data is copied correctly
        assertEquals(source.getId(), target.getId());
        assertEquals(source.getSymbol(), target.getSymbol());
        assertEquals(source.getPrice(), target.getPrice(), 0.0001);
        assertEquals(source.getQuantity(), target.getQuantity());
        assertEquals(source.getSide(), target.getSide());
        assertEquals(source.getType(), target.getType());
        
        // Verify they are different object instances
        assertNotSame(source, target);
        
        // Verify changes to source don't affect target
        source.setQuantity(9999);
        assertNotEquals(source.getQuantity(), target.getQuantity());
    }
}