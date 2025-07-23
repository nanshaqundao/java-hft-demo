package com.hft.memory.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderRecordTest {
    
    @Test
    void testRecordCreation() {
        OrderRecord order = OrderRecord.create(1, "EURUSD", 1.1234, 1000, (byte)1);
        
        assertEquals(1, order.id());
        assertEquals("EURUSD", order.symbol());
        assertEquals(1.1234, order.price(), 0.0001);
        assertEquals(1000, order.quantity());
        assertEquals(1, order.side());
        assertEquals(2, order.type()); // Default LIMIT
        assertTrue(order.timestamp() > 0);
    }
    
    @Test
    void testRecordValidation() {
        // Invalid price
        assertThrows(IllegalArgumentException.class, () -> 
            new OrderRecord(1, "EURUSD", -1.0, 1000, (byte)1, (byte)2, System.nanoTime()));
        
        // Invalid quantity
        assertThrows(IllegalArgumentException.class, () ->
            new OrderRecord(1, "EURUSD", 1.1234, 0, (byte)1, (byte)2, System.nanoTime()));
        
        // Invalid symbol
        assertThrows(IllegalArgumentException.class, () ->
            new OrderRecord(1, null, 1.1234, 1000, (byte)1, (byte)2, System.nanoTime()));
        
        assertThrows(IllegalArgumentException.class, () ->
            new OrderRecord(1, "", 1.1234, 1000, (byte)1, (byte)2, System.nanoTime()));
    }
    
    @Test
    void testConvenienceMethods() {
        OrderRecord buyOrder = OrderRecord.create(1, "EURUSD", 1.1234, 1000, (byte)1);
        OrderRecord sellOrder = OrderRecord.create(2, "GBPUSD", 1.5678, 2000, (byte)2);
        
        assertTrue(buyOrder.isBuy());
        assertFalse(buyOrder.isSell());
        assertEquals("BUY", buyOrder.getSideString());
        
        assertFalse(sellOrder.isBuy());
        assertTrue(sellOrder.isSell());
        assertEquals("SELL", sellOrder.getSideString());
        
        assertEquals("LIMIT", buyOrder.getTypeString());
    }
    
    @Test
    void testRecordEquality() {
        OrderRecord order1 = OrderRecord.create(1, "EURUSD", 1.1234, 1000, (byte)1);
        // Sleep to ensure different timestamp
        try { Thread.sleep(1); } catch (InterruptedException e) {}
        OrderRecord order2 = OrderRecord.create(1, "EURUSD", 1.1234, 1000, (byte)1);
        
        // Records with different timestamps should not be equal
        assertNotEquals(order1, order2);
        
        // Records with same data should be equal
        OrderRecord order3 = new OrderRecord(1, "EURUSD", 1.1234, 1000, (byte)1, (byte)2, 12345L);
        OrderRecord order4 = new OrderRecord(1, "EURUSD", 1.1234, 1000, (byte)1, (byte)2, 12345L);
        assertEquals(order3, order4);
        assertEquals(order3.hashCode(), order4.hashCode());
    }
    
    @Test
    void testToString() {
        OrderRecord order = OrderRecord.create(1, "EURUSD", 1.1234, 1000, (byte)1);
        String str = order.toString();
        
        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("symbol='EURUSD'"));
        assertTrue(str.contains("price=1.1234"));
        assertTrue(str.contains("qty=1000"));
        assertTrue(str.contains("side=BUY"));
    }
    
    @Test
    void testFormattedStringMethod() {
        OrderRecord order = OrderRecord.create(123, "GBPUSD", 1.5678, 2500, (byte)2);
        String formatted = order.toString();
        
        // Java 21 formatted() method should work
        String expected = "OrderRecord{id=%d, symbol='%s', price=%.4f, qty=%d, side=%s}"
            .formatted(123, "GBPUSD", 1.5678, 2500, "SELL");
        
        assertEquals(expected, formatted);
    }
}