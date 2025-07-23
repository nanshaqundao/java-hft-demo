package com.hft.memory.pool;

import com.hft.memory.core.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class ObjectPoolTest {
    
    private ObjectPool<Order> pool;
    
    @BeforeEach
    void setUp() {
        pool = new ObjectPool<>(Order::new, 5);
    }
    
    @Test
    void testPoolInitialization() {
        assertEquals(2, pool.size()); // Pre-warmed with maxSize/2
        assertEquals(5, pool.getMaxSize());
    }
    
    @Test
    void testAcquireAndRelease() {
        Order order1 = pool.acquire();
        assertNotNull(order1);
        assertEquals(1, pool.size()); // One less in pool
        
        pool.release(order1);
        assertEquals(2, pool.size()); // Back in pool
    }
    
    @Test
    void testPoolOverflow() {
        // Fill pool beyond max size
        for (int i = 0; i < 10; i++) {
            Order order = pool.acquire();
            pool.release(order);
        }
        
        // Pool should not exceed max size
        assertTrue(pool.size() <= pool.getMaxSize());
    }
    
    @Test
    void testNullRelease() {
        int initialSize = pool.size();
        pool.release(null);
        assertEquals(initialSize, pool.size()); // No change
    }
    
    @Test
    void testResettableObjects() {
        Order order = pool.acquire();
        order.setId(123);
        order.setSymbol("TEST");
        
        pool.release(order);
        
        Order reacquired = pool.acquire();
        // Should be reset if it's the same object
        if (reacquired == order) {
            assertEquals(0, reacquired.getId());
            assertNull(reacquired.getSymbol());
        }
    }
    
    @Test
    void testConcurrentAccess() throws InterruptedException {
        final int threadCount = 10;
        final int operationsPerThread = 100;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    Order order = pool.acquire();
                    assertNotNull(order);
                    // Simulate some work
                    order.setId(j);
                    pool.release(order);
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Pool should still be valid
        assertTrue(pool.size() <= pool.getMaxSize());
    }
}