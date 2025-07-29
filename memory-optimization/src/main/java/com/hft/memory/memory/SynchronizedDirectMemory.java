package com.hft.memory.memory;

import com.hft.memory.core.Order;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 基于synchronized的直接内存管理策略
 * 
 * 特点：
 * - 使用synchronized关键字保证线程安全
 * - 简单可靠，容易理解和维护
 * - 适合中等并发场景
 * - 两阶段提交保证原子性写入
 * 
 * 性能特征：
 * - 写入延迟：中等（有锁竞争开销）
 * - 读取延迟：中等（有锁竞争开销） 
 * - 吞吐量：中等（串行化访问）
 * - 内存开销：低（无额外数据结构）
 */
public class SynchronizedDirectMemory implements DirectMemoryStrategy {
    
    private final ByteBuffer directBuffer;
    private final int bufferSize;
    private int position;  // 普通int，由synchronized保护
    
    // Fixed length for order serialization
    private static final int ORDER_SERIALIZED_SIZE = 64;
    
    public SynchronizedDirectMemory(int bufferSize) {
        this.bufferSize = bufferSize;
        this.directBuffer = ByteBuffer.allocateDirect(bufferSize);
        this.position = 0;
    }
    
    @Override
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
    
    @Override
    public synchronized Order deserializeOrder(Order reusableOrder, int offset) {
        if (offset + ORDER_SERIALIZED_SIZE > position) {
            return null; // Out of bounds
        }
        
        // synchronized确保不会在写入过程中读取
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
    
    @Override
    public synchronized Order deserializeOrderSafe(int offset) {
        if (offset + ORDER_SERIALIZED_SIZE > position) {
            return null; // Out of bounds
        }
        
        // 创建新的Order对象，避免多线程共享reusableOrder的问题
        Order order = new Order();
        return deserializeOrder(order, offset);
    }
    
    @Override
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
    
    @Override
    public synchronized boolean serializeOrderRing(Order order) {
        // 如果空间不够，直接从头开始覆盖老数据
        if (position + ORDER_SERIALIZED_SIZE > bufferSize) {
            position = 0;  // 简单粗暴：回到开头
        }
        
        return serializeOrderAtPosition(order, position);
    }
    
    @Override
    public synchronized void reset() {
        position = 0;
        directBuffer.clear();
    }
    
    @Override
    public synchronized void forceReset() {
        position = 0;
        // 不清理buffer内容，节省时间
        // 新数据会覆盖旧数据
    }
    
    @Override
    public synchronized int getUsedSize() {
        return position;
    }
    
    @Override
    public synchronized int getRemainingSize() {
        return bufferSize - position;
    }
    
    @Override
    public int getBufferSize() {
        return bufferSize;
    }
    
    @Override
    public synchronized boolean isAlmostFull(double threshold) {
        return (double) position / bufferSize > threshold;
    }
    
    @Override
    public synchronized int getRemainingOrderCapacity() {
        return (bufferSize - position) / ORDER_SERIALIZED_SIZE;
    }
    
    @Override
    public String getStrategyName() {
        return "Synchronized";
    }
    
    @Override
    public String getStrategyDescription() {
        return "基于synchronized的传统并发控制，简单可靠，适合中等并发场景";
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
}