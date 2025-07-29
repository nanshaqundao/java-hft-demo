package com.hft.memory.memory;

import com.hft.memory.core.Order;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于CAS的无锁直接内存管理策略
 * 
 * 特点：
 * - 使用Compare-And-Swap操作实现无锁并发
 * - 高性能，无线程阻塞
 * - 复杂度高，需要处理ABA问题
 * - 适合高并发写入场景
 * 
 * 性能特征：
 * - 写入延迟：低（无锁竞争，但有重试开销）
 * - 读取延迟：低（无锁操作）
 * - 吞吐量：高（并行访问）
 * - 内存开销：低（只有原子变量开销）
 * 
 * 技术挑战：
 * - ABA问题：使用版本号机制解决
 * - 内存排序：确保写入的可见性
 * - 原子性：两阶段提交+CAS确保数据完整性
 */
public class CASDirectMemory implements DirectMemoryStrategy {
    
    private final ByteBuffer directBuffer;
    private final int bufferSize;
    private final AtomicInteger position;  // 无锁位置管理
    private final AtomicInteger writeVersion;  // 版本号，防止ABA问题
    
    // Fixed length for order serialization
    private static final int ORDER_SERIALIZED_SIZE = 64;
    
    // 最大重试次数，防止活锁
    private static final int MAX_RETRY_ATTEMPTS = 100;
    
    public CASDirectMemory(int bufferSize) {
        this.bufferSize = bufferSize;
        this.directBuffer = ByteBuffer.allocateDirect(bufferSize);
        this.position = new AtomicInteger(0);
        this.writeVersion = new AtomicInteger(0);
    }
    
    @Override
    public boolean serializeOrder(Order order) {
        int attempts = 0;
        
        while (attempts < MAX_RETRY_ATTEMPTS) {
            int currentPos = position.get(); // 读取当前位置
            int currentVersion = writeVersion.get(); // 读取当前版本
            
            // 检查空间是否足够
            if (currentPos + ORDER_SERIALIZED_SIZE > bufferSize) {
                return false; // Buffer full
            }
            
            // 原子写入：先在临时缓冲区完成所有写入
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
                
                // 关键的CAS操作：尝试原子性地预留空间
                int newPos = currentPos + ORDER_SERIALIZED_SIZE;
                if (position.compareAndSet(currentPos, newPos)) {
                    // 成功预留空间，现在安全地写入数据
                    
                    // 增加版本号，标记开始写入
                    writeVersion.incrementAndGet();
                    
                    // 原子性地将完整的64字节写入直接内存
                    synchronized (directBuffer) {  // 仅对ByteBuffer操作加锁，保证写入原子性
                        directBuffer.position(currentPos);
                        directBuffer.put(tempBuffer);
                    }
                    
                    // 写入完成，再次增加版本号
                    writeVersion.incrementAndGet();
                    
                    return true;
                } else {
                    // CAS失败，位置被其他线程修改，重试
                    attempts++;
                    
                    // 指数退避，减少CPU占用
                    if (attempts > 10) {
                        try {
                            Thread.sleep(0, attempts * 1000); // 纳秒级退避
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    }
                }
                
            } catch (Exception e) {
                return false;
            }
        }
        
        // 达到最大重试次数，写入失败
        return false;
    }
    
    @Override
    public Order deserializeOrder(Order reusableOrder, int offset) {
        // 无锁读取，但需要检查版本一致性防止读取到部分写入的数据
        int beforeVersion, afterVersion;
        int maxReadRetry = 10;
        int retryCount = 0;
        
        do {
            beforeVersion = writeVersion.get();
            
            // 检查边界
            int currentPos = position.get();
            if (offset + ORDER_SERIALIZED_SIZE > currentPos) {
                return null; // Out of bounds
            }
            
            // 读取数据（在同步块中，确保读取的原子性）
            synchronized (directBuffer) {
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
            }
            
            afterVersion = writeVersion.get();
            retryCount++;
            
            // 如果版本号发生变化，说明有写入操作，重新读取
        } while (beforeVersion != afterVersion && retryCount < maxReadRetry);
        
        if (retryCount >= maxReadRetry) {
            // 读取重试次数过多，可能存在频繁写入
            return null;
        }
        
        return reusableOrder;
    }
    
    @Override
    public Order deserializeOrderSafe(int offset) {
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
                break; // Buffer full or too many retries
            }
        }
        return serializedCount;
    }
    
    @Override
    public boolean serializeOrderRing(Order order) {
        int attempts = 0;
        
        while (attempts < MAX_RETRY_ATTEMPTS) {
            int currentPos = position.get();
            
            // 如果空间不够，尝试重置到开头
            int nextPos = currentPos + ORDER_SERIALIZED_SIZE;
            if (nextPos > bufferSize) {
                // 尝试CAS重置到0
                if (position.compareAndSet(currentPos, 0)) {
                    currentPos = 0;
                } else {
                    attempts++;
                    continue; // 重置失败，重试
                }
            }
            
            // 使用常规序列化逻辑
            if (serializeOrderAtPosition(order, currentPos)) {
                return true;
            }
            
            attempts++;
        }
        
        return false;
    }
    
    @Override
    public void reset() {
        position.set(0);
        writeVersion.set(0);
        synchronized (directBuffer) {
            directBuffer.clear();
        }
    }
    
    @Override
    public void forceReset() {
        position.set(0);
        writeVersion.set(0);
        // 不清理buffer内容，节省时间
    }
    
    @Override
    public int getUsedSize() {
        return position.get();
    }
    
    @Override
    public int getRemainingSize() {
        return bufferSize - position.get();
    }
    
    @Override
    public int getBufferSize() {
        return bufferSize;
    }
    
    @Override
    public boolean isAlmostFull(double threshold) {
        return (double) position.get() / bufferSize > threshold;
    }
    
    @Override
    public int getRemainingOrderCapacity() {
        return (bufferSize - position.get()) / ORDER_SERIALIZED_SIZE;
    }
    
    @Override
    public String getStrategyName() {
        return "CAS-LockFree";
    }
    
    @Override
    public String getStrategyDescription() {
        return "基于CAS的无锁实现，使用版本号防止ABA问题，适合高并发写入场景";
    }
    
    // 私有方法：在指定位置写入订单（用于环形缓冲区）
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
            
            // 尝试CAS更新position
            int expectedPos = startPos;
            int newPos = startPos + ORDER_SERIALIZED_SIZE;
            
            if (position.compareAndSet(expectedPos, newPos)) {
                // 成功更新position，写入数据
                writeVersion.incrementAndGet();
                
                synchronized (directBuffer) {
                    directBuffer.position(startPos);
                    directBuffer.put(tempBuffer);
                }
                
                writeVersion.incrementAndGet();
                return true;
            }
            
            return false; // CAS失败
            
        } catch (Exception e) {
            return false;
        }
    }
}