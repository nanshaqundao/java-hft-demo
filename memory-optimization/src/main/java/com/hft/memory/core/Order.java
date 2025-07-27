package com.hft.memory.core;

import com.hft.memory.pool.Resettable;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class Order implements Resettable, Serializable {
    private static final long serialVersionUID = 1L;
    // Use compact data types
    private int id;                    // 4 bytes
    private volatile long priceAndQty; // 8 bytes (price: high 32 bits, qty: low 32 bits)
    private long timestamp;            // 8 bytes
    private byte side;                 // 1 byte (BUY=1, SELL=2)
    private byte type;                 // 1 byte (MARKET=1, LIMIT=2)
    
    // Use constant strings to avoid repeated creation
    private static final String[] COMMON_SYMBOLS = {
        "EURUSD", "GBPUSD", "USDJPY", "AUDUSD", "USDCAD"
    };
    private byte symbolIndex = -1;     // Index to constant array
    private String customSymbol;       // For non-common symbols
    
    public Order() {}
    
    // High-performance constructor
    public Order(int id, String symbol, double price, int quantity) {
        this.id = id;
        setSymbol(symbol);
        setPriceAndQuantity(price, quantity);
        this.timestamp = System.nanoTime();
        this.side = 1; // BUY
        this.type = 2; // LIMIT
    }
    
    // Copy constructor for safe object duplication
    public Order(Order other) {
        if (other != null) {
            copyFrom(other);
        }
    }
    
    // Copy data from another Order instance (for cache persistence)
    public void copyFrom(Order other) {
        this.id = other.id;
        this.priceAndQty = other.priceAndQty;
        this.timestamp = other.timestamp;
        this.side = other.side;
        this.type = other.type;
        this.symbolIndex = other.symbolIndex;
        this.customSymbol = other.customSymbol;
    }
    
    // Bit manipulation optimization: pack price and quantity into one long
    public void setPriceAndQuantity(double price, int quantity) {
        long priceBits = Double.doubleToRawLongBits(price);
        this.priceAndQty = (priceBits & 0xFFFFFFFF00000000L) | 
                          (quantity & 0xFFFFFFFFL);
    }
    
    public double getPrice() {
        long priceBits = priceAndQty & 0xFFFFFFFF00000000L;
        return Double.longBitsToDouble(priceBits);
    }
    
    public int getQuantity() {
        return (int) (priceAndQty & 0xFFFFFFFFL);
    }
    
    // Symbol optimization: use index for common symbols, reduce String objects
    public void setSymbol(String symbol) {
        for (int i = 0; i < COMMON_SYMBOLS.length; i++) {
            if (COMMON_SYMBOLS[i].equals(symbol)) {
                this.symbolIndex = (byte) i;
                this.customSymbol = null;
                return;
            }
        }
        this.symbolIndex = -1;
        this.customSymbol = symbol.intern(); // Use string pool
    }
    
    public String getSymbol() {
        return symbolIndex >= 0 ? 
               COMMON_SYMBOLS[symbolIndex] : 
               customSymbol;
    }
    
    @Override
    public void reset() {
        this.id = 0;
        this.priceAndQty = 0L;
        this.timestamp = 0L;
        this.side = 1;
        this.type = 2;
        this.symbolIndex = -1;
        this.customSymbol = null;
    }
    
    // High-performance equals/hashCode
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Order)) return false;
        Order other = (Order) obj;
        return this.id == other.id; // Assume ID is unique
    }
    
    @Override
    public int hashCode() {
        return id; // Simple and efficient hash
    }
    
    // High-performance toString using ThreadLocal StringBuilder
    private static final ThreadLocal<StringBuilder> STRING_BUILDER = 
        ThreadLocal.withInitial(() -> new StringBuilder(128));
    
    @Override
    public String toString() {
        StringBuilder sb = STRING_BUILDER.get();
        sb.setLength(0); // Reset instead of creating new
        
        return sb.append("Order{id=").append(id)
                 .append(", symbol='").append(getSymbol())
                 .append("', price=").append(getPrice())
                 .append(", qty=").append(getQuantity())
                 .append(", side=").append(side == 1 ? "BUY" : "SELL")
                 .append('}').toString();
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public byte getSide() { return side; }
    public void setSide(byte side) { this.side = side; }
    
    public byte getType() { return type; }
    public void setType(byte type) { this.type = type; }
    
    public void setPrice(double price) {
        setPriceAndQuantity(price, getQuantity());
    }
    
    public void setQuantity(int quantity) {
        setPriceAndQuantity(getPrice(), quantity);
    }
}