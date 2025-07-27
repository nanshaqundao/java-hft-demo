package com.hft.memory.memory;

import com.hft.memory.core.Order;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DirectMemoryManager {
    private final ByteBuffer directBuffer;
    private final int bufferSize;
    private int position;  // 改为普通int，由synchronized保证线程安全
    
    // Fixed length for order serialization
    private static final int ORDER_SERIALIZED_SIZE = 64;
    
    public DirectMemoryManager(int bufferSize) {
        this.bufferSize = bufferSize;
        this.directBuffer = ByteBuffer.allocateDirect(bufferSize);
        this.position = 0;  // 普通int初始化
    }
    
    // High-performance order serialization with thread safety
    public synchronized boolean serializeOrder(Order order) {
        // 检查空间是否足够
        if (position + ORDER_SERIALIZED_SIZE > bufferSize) {
            return false; // Buffer full
        }
        
        int startPos = position;
        
        // 原子写入：先在临时缓冲区完成所有写入，再一次性复制到直接内存
        byte[] tempBuffer = new byte[ORDER_SERIALIZED_SIZE];
        ByteBuffer temp = ByteBuffer.wrap(tempBuffer);
        
        try {
            // 在临时缓冲区中完成所有写入操作
            temp.putInt(0, order.getId());
            temp.putLong(4, Double.doubleToRawLongBits(order.getPrice()));
            temp.putInt(12, order.getQuantity());
            temp.putLong(16, order.getTimestamp());
            temp.put(24, order.getSide());
            temp.put(25, order.getType());
            
            // Symbol serialization (max 32 bytes)
            String symbol = order.getSymbol();
            if (symbol != null) {
                byte[] symbolBytes = symbol.getBytes(StandardCharsets.UTF_8);
                int symbolLength = Math.min(symbolBytes.length, 31);
                temp.put(26, (byte) symbolLength);
                for (int i = 0; i < symbolLength; i++) {
                    temp.put(27 + i, symbolBytes[i]);
                }
                // 剩余字节填零
                for (int i = 27 + symbolLength; i < ORDER_SERIALIZED_SIZE; i++) {
                    temp.put(i, (byte) 0);
                }
            } else {
                temp.put(26, (byte) 0);
                // 剩余字节填零
                for (int i = 27; i < ORDER_SERIALIZED_SIZE; i++) {
                    temp.put(i, (byte) 0);
                }
            }
            
            // 原子性地将完整的64字节写入直接内存
            directBuffer.position(startPos);
            directBuffer.put(tempBuffer);  // 一次性写入，保证原子性
            
            // 只有写入成功后才更新position
            position += ORDER_SERIALIZED_SIZE;
            
            return true;
        } catch (Exception e) {
            // 写入失败，position无需回滚（因为还没有更新）
            return false;
        }
    }
    
    // High-performance order deserialization with thread safety
    public synchronized Order deserializeOrder(Order reusableOrder, int offset) {
        if (offset + ORDER_SERIALIZED_SIZE > position) {
            return null; // Out of bounds
        }
        
        // 问题4解决方案：加锁防止读取到部分写入的数据
        // synchronized确保不会在写入过程中读取
        
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
    
    // 问题4解决方案：提供线程安全的反序列化，每次创建新Order对象
    public synchronized Order deserializeOrderSafe(int offset) {
        if (offset + ORDER_SERIALIZED_SIZE > position) {
            return null; // Out of bounds
        }
        
        // 创建新的Order对象，避免多线程共享reusableOrder的问题
        Order order = new Order();
        return deserializeOrder(order, offset);
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
    // HFT优化：简单粗暴的内存管理
    public synchronized void reset() {
        position = 0;
        directBuffer.clear();
    }
    
    // HFT风格：环形缓冲区，延迟可预测
    public synchronized boolean serializeOrderRing(Order order) {
        // 如果空间不够，直接从头开始覆盖老数据
        if (position + ORDER_SERIALIZED_SIZE > bufferSize) {
            position = 0;  // 简单粗暴：回到开头
        }
        
        return serializeOrderAtPosition(order, position);
    }
    
    // HFT风格：强制重置，延迟恒定
    public synchronized void forceReset() {
        position = 0;
        // 不清理buffer内容，节省时间
        // 新数据会覆盖旧数据
    }
    
    // 私有方法：在指定位置写入订单
    private boolean serializeOrderAtPosition(Order order, int startPos) {
        byte[] tempBuffer = new byte[ORDER_SERIALIZED_SIZE];
        ByteBuffer temp = ByteBuffer.wrap(tempBuffer);
        
        try {
            // 在临时缓冲区中完成所有写入操作
            temp.putInt(0, order.getId());
            temp.putLong(4, Double.doubleToRawLongBits(order.getPrice()));
            temp.putInt(12, order.getQuantity());
            temp.putLong(16, order.getTimestamp());
            temp.put(24, order.getSide());
            temp.put(25, order.getType());
            
            // Symbol serialization
            String symbol = order.getSymbol();
            if (symbol != null) {
                byte[] symbolBytes = symbol.getBytes(StandardCharsets.UTF_8);
                int symbolLength = Math.min(symbolBytes.length, 31);
                temp.put(26, (byte) symbolLength);
                for (int i = 0; i < symbolLength; i++) {
                    temp.put(27 + i, symbolBytes[i]);
                }
                for (int i = 27 + symbolLength; i < ORDER_SERIALIZED_SIZE; i++) {
                    temp.put(i, (byte) 0);
                }
            } else {
                temp.put(26, (byte) 0);
                for (int i = 27; i < ORDER_SERIALIZED_SIZE; i++) {
                    temp.put(i, (byte) 0);
                }
            }
            
            // 原子性写入
            directBuffer.position(startPos);
            directBuffer.put(tempBuffer);
            
            // 更新position
            position = startPos + ORDER_SERIALIZED_SIZE;
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // Get used size
    public synchronized int getUsedSize() {
        return position;
    }
    
    // Get remaining space
    public synchronized int getRemainingSize() {
        return bufferSize - position;
    }
    
    public int getBufferSize() {
        return bufferSize;
    }
    
    // HFT工具：检查是否接近满载
    public synchronized boolean isAlmostFull(double threshold) {
        return (double) position / bufferSize > threshold;
    }
    
    // HFT工具：获取可存储的剩余订单数量
    public synchronized int getRemainingOrderCapacity() {
        return (bufferSize - position) / ORDER_SERIALIZED_SIZE;
    }
}