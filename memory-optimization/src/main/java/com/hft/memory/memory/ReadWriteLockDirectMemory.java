package com.hft.memory.memory;

import com.hft.memory.core.Order;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基于读写锁的直接内存管理策略
 * 
 * 特点：
 * - 使用ReadWriteLock分离读写操作
 * - 多个读操作可以并发执行
 * - 写操作需要独占访问
 * - 适合读多写少的场景
 * 
 * 性能特征：
 * - 写入延迟：中等（需要等待所有读操作完成）
 * - 读取延迟：低（读操作间可以并发）
 * - 吞吐量：读多写少场景下高，写多场景下中等
 * - 内存开销：中等（ReadWriteLock内部状态）
 * 
 * 适用场景：
 * - 频繁查询历史订单数据
 * - 批量反序列化操作
 * - 多线程读取，少量写入的工作负载
 */
public class ReadWriteLockDirectMemory implements DirectMemoryStrategy {
    
    private final ByteBuffer directBuffer;
    private final int bufferSize;
    private volatile int position;  // volatile保证可见性，读写锁保证原子性
    private final ReadWriteLock rwLock;
    
    // Fixed length for order serialization
    private static final int ORDER_SERIALIZED_SIZE = 64;
    
    public ReadWriteLockDirectMemory(int bufferSize) {
        this.bufferSize = bufferSize;
        this.directBuffer = ByteBuffer.allocateDirect(bufferSize);
        this.position = 0;
        this.rwLock = new ReentrantReadWriteLock(false); // 非公平锁，提高性能
    }
    
    @Override
    public boolean serializeOrder(Order order) {
        rwLock.writeLock().lock(); // 获取写锁，独占访问
        try {
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
            
        } finally {
            rwLock.writeLock().unlock(); // 确保释放写锁
        }
    }
    
    @Override
    public Order deserializeOrder(Order reusableOrder, int offset) {
        rwLock.readLock().lock(); // 获取读锁，允许并发读取
        try {
            // 检查边界
            if (offset + ORDER_SERIALIZED_SIZE > position) {
                return null; // Out of bounds
            }
            
            // 在读锁保护下安全读取数据
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
            
        } finally {
            rwLock.readLock().unlock(); // 确保释放读锁
        }
    }
    
    @Override
    public Order deserializeOrderSafe(int offset) {
        // 创建新的Order对象，避免多线程共享reusableOrder的问题
        Order order = new Order();
        return deserializeOrder(order, offset);
    }
    
    @Override
    public int serializeOrderBatch(List<Order> orders) {
        // 批量写入时，一次性获取写锁，减少锁竞争开销
        rwLock.writeLock().lock();
        try {
            int serializedCount = 0;
            for (Order order : orders) {
                // 内部调用不需要再次获取锁
                if (serializeOrderInternal(order)) {
                    serializedCount++;
                } else {
                    break; // Buffer full
                }
            }
            return serializedCount;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean serializeOrderRing(Order order) {
        rwLock.writeLock().lock();
        try {
            // 如果空间不够，直接从头开始覆盖老数据
            if (position + ORDER_SERIALIZED_SIZE > bufferSize) {
                position = 0;  // 简单粗暴：回到开头
            }
            
            return serializeOrderAtPosition(order, position);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    @Override
    public void reset() {
        rwLock.writeLock().lock();
        try {
            position = 0;
            directBuffer.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    @Override
    public void forceReset() {
        rwLock.writeLock().lock();
        try {
            position = 0;
            // 不清理buffer内容，节省时间
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    @Override
    public int getUsedSize() {
        rwLock.readLock().lock();
        try {
            return position;
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public int getRemainingSize() {
        rwLock.readLock().lock();
        try {
            return bufferSize - position;
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public int getBufferSize() {
        return bufferSize; // 常量，无需加锁
    }
    
    @Override
    public boolean isAlmostFull(double threshold) {
        rwLock.readLock().lock();
        try {
            return (double) position / bufferSize > threshold;
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public int getRemainingOrderCapacity() {
        rwLock.readLock().lock();
        try {
            return (bufferSize - position) / ORDER_SERIALIZED_SIZE;
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public String getStrategyName() {
        return "ReadWriteLock";
    }
    
    @Override
    public String getStrategyDescription() {
        return "基于读写锁的并发控制，读操作并发，写操作独占，适合读多写少场景";
    }
    
    // 私有方法：内部序列化（已在写锁保护下）
    private boolean serializeOrderInternal(Order order) {
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
            position += ORDER_SERIALIZED_SIZE;
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // 私有方法：在指定位置写入订单（已在写锁保护下）
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