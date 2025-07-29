package com.hft.memory.memory;

import com.hft.memory.core.Order;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于分段锁的直接内存管理策略
 * 
 * 特点：
 * - 将缓冲区分为多个段，每个段有独立的锁
 * - 减少锁竞争，提高并发性能
 * - 使用轮询策略分配写入段
 * - 读取时需要遍历所有段
 * 
 * 性能特征：
 * - 写入延迟：低（锁竞争减少）
 * - 读取延迟：中等（需要跨段查找）
 * - 吞吐量：高（多段并行写入）
 * - 内存开销：中等（每段都有独立状态）
 * 
 * 适用场景：
 * - 高并发写入，读取相对较少
 * - 多线程写入密集型工作负载
 * - 对写入性能要求极高的场景
 * 
 * 设计思想：
 * - 借鉴ConcurrentHashMap（Java 7）的分段锁思想
 * - 平衡锁粒度和管理复杂度
 */
public class SegmentedLockDirectMemory implements DirectMemoryStrategy {
    
    // 段的数量，通常设置为CPU核心数的2-4倍
    private static final int SEGMENT_COUNT = 16;
    private static final int ORDER_SERIALIZED_SIZE = 64;
    
    private final ByteBuffer directBuffer;
    private final int bufferSize;
    private final Segment[] segments;
    private final AtomicInteger writeCounter; // 用于轮询选择段
    
    /**
     * 单个段的定义
     * 每个段管理缓冲区的一部分，有独立的锁和位置指针
     */
    private static class Segment {
        private final ReentrantLock lock = new ReentrantLock();
        private final int startOffset;  // 段在整个缓冲区中的起始位置
        private final int segmentSize;  // 段的大小
        private volatile int position;  // 段内的当前位置
        
        public Segment(int startOffset, int segmentSize) {
            this.startOffset = startOffset;
            this.segmentSize = segmentSize;
            this.position = 0;
        }
        
        public void lock() {
            lock.lock();
        }
        
        public void unlock() {
            lock.unlock();
        }
        
        public boolean tryLock() {
            return lock.tryLock();
        }
        
        public int getAbsolutePosition() {
            return startOffset + position;
        }
        
        public boolean hasSpace() {
            return position + ORDER_SERIALIZED_SIZE <= segmentSize;
        }
        
        public void advancePosition() {
            position += ORDER_SERIALIZED_SIZE;
        }
        
        public void reset() {
            position = 0;
        }
        
        public int getUsedSize() {
            return position;
        }
        
        public int getRemainingSize() {
            return segmentSize - position;
        }
    }
    
    public SegmentedLockDirectMemory(int bufferSize) {
        this.bufferSize = bufferSize;
        this.directBuffer = ByteBuffer.allocateDirect(bufferSize);
        this.segments = new Segment[SEGMENT_COUNT];
        this.writeCounter = new AtomicInteger(0);
        
        // 初始化段
        int segmentSize = bufferSize / SEGMENT_COUNT;
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            int startOffset = i * segmentSize;
            // 最后一个段包含所有剩余空间
            int actualSegmentSize = (i == SEGMENT_COUNT - 1) ? 
                bufferSize - startOffset : segmentSize;
            segments[i] = new Segment(startOffset, actualSegmentSize);
        }
    }
    
    @Override
    public boolean serializeOrder(Order order) {
        // 使用轮询策略选择段，减少锁竞争
        int startSegment = writeCounter.getAndIncrement() % SEGMENT_COUNT;
        
        // 尝试在选定的段中写入，如果失败则尝试其他段
        for (int attempt = 0; attempt < SEGMENT_COUNT; attempt++) {
            int segmentIndex = (startSegment + attempt) % SEGMENT_COUNT;
            Segment segment = segments[segmentIndex];
            
            // 尝试获取段锁
            if (segment.tryLock()) {
                try {
                    // 检查段是否有足够空间
                    if (!segment.hasSpace()) {
                        continue; // 这个段已满，尝试下一个段
                    }
                    
                    // 在此段中序列化订单
                    int absolutePos = segment.getAbsolutePosition();
                    if (writeOrderToBuffer(order, absolutePos)) {
                        segment.advancePosition();
                        return true;
                    }
                    
                } finally {
                    segment.unlock();
                }
            }
            // 如果无法获取锁或写入失败，尝试下一个段
        }
        
        // 所有段都已满或繁忙
        return false;
    }
    
    @Override
    public Order deserializeOrder(Order reusableOrder, int offset) {
        // 确定offset属于哪个段
        Segment targetSegment = findSegmentByOffset(offset);
        if (targetSegment == null) {
            return null; // Invalid offset
        }
        
        targetSegment.lock();
        try {
            // 检查offset是否在有效范围内
            if (offset + ORDER_SERIALIZED_SIZE > targetSegment.getAbsolutePosition()) {
                return null; // Out of bounds
            }
            
            return readOrderFromBuffer(reusableOrder, offset);
            
        } finally {
            targetSegment.unlock();
        }
    }
    
    @Override
    public Order deserializeOrderSafe(int offset) {
        Order order = new Order();
        return deserializeOrder(order, offset);
    }
    
    @Override
    public int serializeOrderBatch(List<Order> orders) {
        int serializedCount = 0;
        
        // 为了避免复杂的跨段管理，批量写入时选择空间最大的段
        Segment bestSegment = findBestSegmentForBatch(orders.size());
        if (bestSegment == null) {
            return 0; // No segment has enough space
        }
        
        bestSegment.lock();
        try {
            for (Order order : orders) {
                if (!bestSegment.hasSpace()) {
                    break; // Segment full
                }
                
                int absolutePos = bestSegment.getAbsolutePosition();
                if (writeOrderToBuffer(order, absolutePos)) {
                    bestSegment.advancePosition();
                    serializedCount++;
                } else {
                    break; // Write failed
                }
            }
        } finally {
            bestSegment.unlock();
        }
        
        return serializedCount;
    }
    
    @Override
    public boolean serializeOrderRing(Order order) {
        // 环形缓冲区：当段满时重置该段
        int segmentIndex = writeCounter.getAndIncrement() % SEGMENT_COUNT;
        Segment segment = segments[segmentIndex];
        
        segment.lock();
        try {
            // 如果段满了，重置它
            if (!segment.hasSpace()) {
                segment.reset(); // 简单粗暴：重置整个段
            }
            
            int absolutePos = segment.getAbsolutePosition();
            if (writeOrderToBuffer(order, absolutePos)) {
                segment.advancePosition();
                return true;
            }
            
            return false;
            
        } finally {
            segment.unlock();
        }
    }
    
    @Override
    public void reset() {
        // 重置所有段
        for (Segment segment : segments) {
            segment.lock();
            try {
                segment.reset();
            } finally {
                segment.unlock();
            }
        }
        directBuffer.clear();
    }
    
    @Override
    public void forceReset() {
        // 强制重置所有段，不清理buffer内容
        for (Segment segment : segments) {
            segment.lock();
            try {
                segment.reset();
            } finally {
                segment.unlock();
            }
        }
    }
    
    @Override
    public int getUsedSize() {
        int totalUsed = 0;
        for (Segment segment : segments) {
            segment.lock();
            try {
                totalUsed += segment.getUsedSize();
            } finally {
                segment.unlock();
            }
        }
        return totalUsed;
    }
    
    @Override
    public int getRemainingSize() {
        int totalRemaining = 0;
        for (Segment segment : segments) {
            segment.lock();
            try {
                totalRemaining += segment.getRemainingSize();
            } finally {
                segment.unlock();
            }
        }
        return totalRemaining;
    }
    
    @Override
    public int getBufferSize() {
        return bufferSize;
    }
    
    @Override
    public boolean isAlmostFull(double threshold) {
        int totalUsed = getUsedSize();
        return (double) totalUsed / bufferSize > threshold;
    }
    
    @Override
    public int getRemainingOrderCapacity() {
        return getRemainingSize() / ORDER_SERIALIZED_SIZE;
    }
    
    @Override
    public String getStrategyName() {
        return "SegmentedLock";
    }
    
    @Override
    public String getStrategyDescription() {
        return String.format("基于分段锁的并发控制，%d个段并行，减少锁竞争，适合高并发写入场景", SEGMENT_COUNT);
    }
    
    // 私有辅助方法
    
    /**
     * 将订单写入缓冲区的指定位置
     */
    private boolean writeOrderToBuffer(Order order, int position) {
        // 使用两阶段提交保证原子性
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
            directBuffer.position(position);
            directBuffer.put(tempBuffer);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从缓冲区的指定位置读取订单
     */
    private Order readOrderFromBuffer(Order reusableOrder, int offset) {
        try {
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
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 根据offset找到对应的段
     */
    private Segment findSegmentByOffset(int offset) {
        for (Segment segment : segments) {
            if (offset >= segment.startOffset && 
                offset < segment.startOffset + segment.segmentSize) {
                return segment;
            }
        }
        return null;
    }
    
    /**
     * 为批量写入找到最适合的段
     */
    private Segment findBestSegmentForBatch(int orderCount) {
        int requiredSpace = orderCount * ORDER_SERIALIZED_SIZE;
        Segment bestSegment = null;
        int maxSpace = 0;
        
        for (Segment segment : segments) {
            int availableSpace = segment.getRemainingSize();
            if (availableSpace >= requiredSpace) {
                return segment; // 找到足够空间的段，直接使用
            }
            if (availableSpace > maxSpace) {
                maxSpace = availableSpace;
                bestSegment = segment;
            }
        }
        
        return maxSpace > 0 ? bestSegment : null;
    }
}