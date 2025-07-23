package com.hft.memory.memory;

import com.hft.memory.core.Order;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DirectMemoryManager {
    private final ByteBuffer directBuffer;
    private final int bufferSize;
    private volatile int position;
    
    // Fixed length for order serialization
    private static final int ORDER_SERIALIZED_SIZE = 64;
    
    public DirectMemoryManager(int bufferSize) {
        this.bufferSize = bufferSize;
        this.directBuffer = ByteBuffer.allocateDirect(bufferSize);
        this.position = 0;
    }
    
    // High-performance order serialization
    public boolean serializeOrder(Order order) {
        if (directBuffer.remaining() < ORDER_SERIALIZED_SIZE) {
            return false; // Buffer full
        }
        
        // Use relative position operations to avoid position synchronization overhead
        int startPos = position;
        
        directBuffer.putInt(startPos, order.getId());
        directBuffer.putLong(startPos + 4, 
            Double.doubleToRawLongBits(order.getPrice()));
        directBuffer.putInt(startPos + 12, order.getQuantity());
        directBuffer.putLong(startPos + 16, order.getTimestamp());
        directBuffer.put(startPos + 24, order.getSide());
        directBuffer.put(startPos + 25, order.getType());
        
        // Symbol serialization (max 32 bytes)
        String symbol = order.getSymbol();
        byte[] symbolBytes = symbol.getBytes(StandardCharsets.UTF_8);
        int symbolLength = Math.min(symbolBytes.length, 31);
        directBuffer.put(startPos + 26, (byte) symbolLength);
        directBuffer.position(startPos + 27);
        directBuffer.put(symbolBytes, 0, symbolLength);
        
        // Update position
        position += ORDER_SERIALIZED_SIZE;
        return true;
    }
    
    // High-performance order deserialization
    public Order deserializeOrder(Order reusableOrder, int offset) {
        if (offset + ORDER_SERIALIZED_SIZE > position) {
            return null; // Out of bounds
        }
        
        reusableOrder.setId(directBuffer.getInt(offset));
        
        double price = Double.longBitsToDouble(
            directBuffer.getLong(offset + 4));
        int quantity = directBuffer.getInt(offset + 12);
        reusableOrder.setPriceAndQuantity(price, quantity);
        
        reusableOrder.setTimestamp(directBuffer.getLong(offset + 16));
        reusableOrder.setSide(directBuffer.get(offset + 24));
        reusableOrder.setType(directBuffer.get(offset + 25));
        
        // Symbol deserialization
        int symbolLength = directBuffer.get(offset + 26) & 0xFF;
        byte[] symbolBytes = new byte[symbolLength];
        directBuffer.position(offset + 27);
        directBuffer.get(symbolBytes, 0, symbolLength);
        String symbol = new String(symbolBytes, StandardCharsets.UTF_8);
        reusableOrder.setSymbol(symbol);
        
        return reusableOrder;
    }
    
    // Batch serialization
    public int serializeOrderBatch(List<Order> orders) {
        int serializedCount = 0;
        for (Order order : orders) {
            if (serializeOrder(order)) {
                serializedCount++;
            } else {
                break; // Buffer full
            }
        }
        return serializedCount;
    }
    
    // Reset buffer
    public void reset() {
        position = 0;
        directBuffer.clear();
    }
    
    // Get used size
    public int getUsedSize() {
        return position;
    }
    
    // Get remaining space
    public int getRemainingSize() {
        return bufferSize - position;
    }
    
    public int getBufferSize() {
        return bufferSize;
    }
}