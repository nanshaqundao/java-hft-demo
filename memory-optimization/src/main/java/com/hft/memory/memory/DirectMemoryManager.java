package com.hft.memory.memory;

import com.hft.memory.core.Order;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DirectMemoryManager {
    private final ByteBuffer directBuffer;
    private final int bufferSize;
    private final AtomicInteger position;
    
    // Fixed length for order serialization
    private static final int ORDER_SERIALIZED_SIZE = 64;
    
    public DirectMemoryManager(int bufferSize) {
        this.bufferSize = bufferSize;
        this.directBuffer = ByteBuffer.allocateDirect(bufferSize);
        this.position = new AtomicInteger(0);
    }
    
    // High-performance order serialization with thread safety
    public synchronized boolean serializeOrder(Order order) {
        int currentPos = position.get();
        if (currentPos + ORDER_SERIALIZED_SIZE > bufferSize) {
            return false; // Buffer full
        }
        
        // Atomically reserve space
        int startPos = position.getAndAdd(ORDER_SERIALIZED_SIZE);
        if (startPos + ORDER_SERIALIZED_SIZE > bufferSize) {
            // Rollback if buffer overflow
            position.addAndGet(-ORDER_SERIALIZED_SIZE);
            return false;
        }
        
        try {
            directBuffer.putInt(startPos, order.getId());
            directBuffer.putLong(startPos + 4, 
                Double.doubleToRawLongBits(order.getPrice()));
            directBuffer.putInt(startPos + 12, order.getQuantity());
            directBuffer.putLong(startPos + 16, order.getTimestamp());
            directBuffer.put(startPos + 24, order.getSide());
            directBuffer.put(startPos + 25, order.getType());
            
            // Symbol serialization (max 32 bytes)
            String symbol = order.getSymbol();
            if (symbol != null) {
                byte[] symbolBytes = symbol.getBytes(StandardCharsets.UTF_8);
                int symbolLength = Math.min(symbolBytes.length, 31);
                directBuffer.put(startPos + 26, (byte) symbolLength);
                for (int i = 0; i < symbolLength; i++) {
                    directBuffer.put(startPos + 27 + i, symbolBytes[i]);
                }
            } else {
                directBuffer.put(startPos + 26, (byte) 0);
            }
            
            return true;
        } catch (Exception e) {
            // Rollback on error
            position.addAndGet(-ORDER_SERIALIZED_SIZE);
            return false;
        }
    }
    
    // High-performance order deserialization
    public Order deserializeOrder(Order reusableOrder, int offset) {
        if (offset + ORDER_SERIALIZED_SIZE > position.get()) {
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
        
        // Symbol deserialization - avoid position() method for thread safety
        int symbolLength = directBuffer.get(offset + 26) & 0xFF;
        if (symbolLength > 0) {
            byte[] symbolBytes = new byte[symbolLength];
            for (int i = 0; i < symbolLength; i++) {
                symbolBytes[i] = directBuffer.get(offset + 27 + i);
            }
            String symbol = new String(symbolBytes, StandardCharsets.UTF_8);
            reusableOrder.setSymbol(symbol);
        } else {
            reusableOrder.setSymbol("");
        }
        
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
    public synchronized void reset() {
        position.set(0);
        directBuffer.clear();
    }
    
    // Get used size
    public int getUsedSize() {
        return position.get();
    }
    
    // Get remaining space
    public int getRemainingSize() {
        return bufferSize - position.get();
    }
    
    public int getBufferSize() {
        return bufferSize;
    }
}