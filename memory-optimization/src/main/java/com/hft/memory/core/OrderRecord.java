package com.hft.memory.core;

/**
 * Java 21 Record-based Order implementation for comparison
 * Records provide automatic equals/hashCode and are more memory efficient
 */
public record OrderRecord(
    int id,
    String symbol, 
    double price,
    int quantity,
    byte side,
    byte type,
    long timestamp
) {
    
    // Compact constructor for validation
    public OrderRecord {
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
    }
    
    // Factory method for creation
    public static OrderRecord create(int id, String symbol, double price, int quantity, byte side) {
        return new OrderRecord(id, symbol, price, quantity, side, (byte) 2, System.nanoTime());
    }
    
    // Convenience methods
    public boolean isBuy() {
        return side == 1;
    }
    
    public boolean isSell() {
        return side == 2;
    }
    
    public String getSideString() {
        return isBuy() ? "BUY" : "SELL";
    }
    
    public String getTypeString() {
        return type == 1 ? "MARKET" : "LIMIT";
    }
    
    @Override
    public String toString() {
        return "OrderRecord{id=%d, symbol='%s', price=%.4f, qty=%d, side=%s}"
            .formatted(id, symbol, price, quantity, getSideString());
    }
}