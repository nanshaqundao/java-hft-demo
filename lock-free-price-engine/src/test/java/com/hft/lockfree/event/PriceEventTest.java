package com.hft.lockfree.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PriceEvent单元测试
 */
class PriceEventTest {
    
    private PriceEvent priceEvent;
    
    @BeforeEach
    void setUp() {
        priceEvent = new PriceEvent();
    }
    
    @Test
    void testDefaultConstructor() {
        assertNull(priceEvent.getSymbol());
        assertEquals(0.0, priceEvent.getBidPrice());
        assertEquals(0.0, priceEvent.getAskPrice());
        assertEquals(0L, priceEvent.getTimestamp());
        assertEquals(0L, priceEvent.getSequence());
        assertFalse(priceEvent.isProcessed());
    }
    
    @Test
    void testSetPriceData() {
        String symbol = "EURUSD";
        double bidPrice = 1.0999;
        double askPrice = 1.1001;
        long timestamp = System.nanoTime();
        
        priceEvent.setPriceData(symbol, bidPrice, askPrice, timestamp);
        
        assertEquals(symbol, priceEvent.getSymbol());
        assertEquals(bidPrice, priceEvent.getBidPrice());
        assertEquals(askPrice, priceEvent.getAskPrice());
        assertEquals(timestamp, priceEvent.getTimestamp());
    }
    
    @Test
    void testSpreadCalculation() {
        priceEvent.setBidPrice(1.0999);
        priceEvent.setAskPrice(1.1001);
        
        assertEquals(0.0002, priceEvent.getSpread(), 0.000001);
    }
    
    @Test
    void testMidPriceCalculation() {
        priceEvent.setBidPrice(1.0999);
        priceEvent.setAskPrice(1.1001);
        
        assertEquals(1.1000, priceEvent.getMidPrice(), 0.000001);
    }
    
    @Test
    void testValidation() {
        // Invalid - no symbol
        assertFalse(priceEvent.isValid());
        
        // Invalid - empty symbol
        priceEvent.setSymbol("");
        assertFalse(priceEvent.isValid());
        
        // Invalid - zero prices
        priceEvent.setSymbol("EURUSD");
        assertFalse(priceEvent.isValid());
        
        // Invalid - bid > ask
        priceEvent.setBidPrice(1.1001);
        priceEvent.setAskPrice(1.0999);
        priceEvent.setTimestamp(System.nanoTime());
        assertFalse(priceEvent.isValid());
        
        // Valid
        priceEvent.setBidPrice(1.0999);
        priceEvent.setAskPrice(1.1001);
        assertTrue(priceEvent.isValid());
    }
    
    @Test
    void testReset() {
        // Set some data
        priceEvent.setPriceData("EURUSD", 1.0999, 1.1001, System.nanoTime());
        priceEvent.setSequence(123);
        priceEvent.setProcessed(true);
        
        // Reset
        priceEvent.reset();
        
        // Verify reset
        assertNull(priceEvent.getSymbol());
        assertEquals(0.0, priceEvent.getBidPrice());
        assertEquals(0.0, priceEvent.getAskPrice());
        assertEquals(0L, priceEvent.getTimestamp());
        assertEquals(0L, priceEvent.getSequence());
        assertFalse(priceEvent.isProcessed());
    }
    
    @Test
    void testCopyFrom() {
        // Create source event
        PriceEvent source = new PriceEvent();
        source.setPriceData("GBPUSD", 1.2999, 1.3001, System.nanoTime());
        source.setSequence(456);
        source.setVolume(1000000);
        source.setProcessed(true);
        
        // Copy to target
        priceEvent.copyFrom(source);
        
        // Verify copy
        assertEquals(source.getSymbol(), priceEvent.getSymbol());
        assertEquals(source.getBidPrice(), priceEvent.getBidPrice());
        assertEquals(source.getAskPrice(), priceEvent.getAskPrice());
        assertEquals(source.getTimestamp(), priceEvent.getTimestamp());
        assertEquals(source.getSequence(), priceEvent.getSequence());
        assertEquals(source.getVolume(), priceEvent.getVolume());
        assertEquals(source.isProcessed(), priceEvent.isProcessed());
    }
    
    @Test
    void testCopyFromNull() {
        // Set some initial data
        priceEvent.setPriceData("EURUSD", 1.0999, 1.1001, System.nanoTime());
        
        // Copy from null should not change anything
        priceEvent.copyFrom(null);
        
        assertEquals("EURUSD", priceEvent.getSymbol());
        assertEquals(1.0999, priceEvent.getBidPrice());
        assertEquals(1.1001, priceEvent.getAskPrice());
    }
    
    @Test
    void testToString() {
        priceEvent.setPriceData("EURUSD", 1.0999, 1.1001, System.nanoTime());
        priceEvent.setSequence(123);
        
        String result = priceEvent.toString();
        
        assertTrue(result.contains("EURUSD"));
        assertTrue(result.contains("1.0999"));
        assertTrue(result.contains("1.1001"));
        assertTrue(result.contains("123"));
    }
}