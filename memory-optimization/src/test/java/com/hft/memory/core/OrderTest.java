package com.hft.memory.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class OrderTest {
    
    private Order order;
    
    @BeforeEach
    void setUp() {
        order = new Order();
    }
    
    @Test
    void testBasicOrderCreation() {
        Order order = new Order(1, "EURUSD", 1.1234, 1000);
        
        assertEquals(1, order.getId());
        assertEquals("EURUSD", order.getSymbol());
        assertEquals(1.1234, order.getPrice(), 0.0001);
        assertEquals(1000, order.getQuantity());
        assertEquals(1, order.getSide()); // BUY
        assertEquals(2, order.getType()); // LIMIT
    }
    
    @Test
    void testPriceAndQuantityPacking() {
        order.setPriceAndQuantity(1.2345, 5000);
        
        assertEquals(1.2345, order.getPrice(), 0.0001);
        assertEquals(5000, order.getQuantity());
    }
    
    @Test
    void testSymbolOptimization() {
        // Test common symbol (should use index)
        order.setSymbol("EURUSD");
        assertEquals("EURUSD", order.getSymbol());
        
        // Test custom symbol (should use intern)
        order.setSymbol("CUSTOM");
        assertEquals("CUSTOM", order.getSymbol());
    }
    
    @Test
    void testReset() {
        order.setId(123);
        order.setSymbol("GBPUSD");
        order.setPriceAndQuantity(1.5, 2000);
        
        order.reset();
        
        assertEquals(0, order.getId());
        assertEquals(0.0, order.getPrice());
        assertEquals(0, order.getQuantity());
        assertNull(order.getSymbol());
    }
    
    @Test
    void testEqualsAndHashCode() {
        Order order1 = new Order(1, "EURUSD", 1.1, 1000);
        Order order2 = new Order(1, "GBPUSD", 1.2, 2000);
        Order order3 = new Order(2, "EURUSD", 1.1, 1000);
        
        assertEquals(order1, order2); // Same ID
        assertNotEquals(order1, order3); // Different ID
        assertEquals(order1.hashCode(), order2.hashCode());
    }
    
    @Test
    void testToString() {
        Order order = new Order(1, "EURUSD", 1.1234, 1000);
        String str = order.toString();
        
        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("symbol='EURUSD'"));
        // Check for approximate price due to floating point precision
        assertTrue(str.contains("price=1.123") || str.contains("price=1.1234"));
        assertTrue(str.contains("qty=1000"));
        assertTrue(str.contains("BUY"));
    }
}