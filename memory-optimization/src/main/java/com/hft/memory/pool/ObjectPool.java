package com.hft.memory.pool;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ObjectPool<T> {
    private final ConcurrentLinkedQueue<T> pool;
    private final Supplier<T> factory;
    private final int maxSize;
    private final AtomicInteger currentSize;
    
    public ObjectPool(Supplier<T> factory, int maxSize) {
        this.factory = factory;
        this.maxSize = maxSize;
        this.pool = new ConcurrentLinkedQueue<>();
        this.currentSize = new AtomicInteger(0);
        
        // Pre-warm the object pool
        for (int i = 0; i < maxSize / 2; i++) {
            pool.offer(factory.get());
            currentSize.incrementAndGet();
        }
    }
    
    public T acquire() {
        T object = pool.poll();
        if (object != null) {
            currentSize.decrementAndGet();
            return object;
        }
        // No object in pool, create new one
        return factory.get();
    }
    
    public void release(T object) {
        if (object == null) return;
        
        // Reset object state
        if (object instanceof Resettable) {
            ((Resettable) object).reset();
        }
        
        // Atomically check and increment size using CAS
        int currentSizeValue;
        do {
            currentSizeValue = currentSize.get();
            if (currentSizeValue >= maxSize) {
                // Pool is full, let GC collect the object
                return;
            }
            // Try to increment atomically - retry if another thread changed it
        } while (!currentSize.compareAndSet(currentSizeValue, currentSizeValue + 1));
        
        // Successfully reserved a slot, add to pool
        pool.offer(object);
    }
    
    public int size() {
        return currentSize.get();
    }
    
    public int getMaxSize() {
        return maxSize;
    }
}