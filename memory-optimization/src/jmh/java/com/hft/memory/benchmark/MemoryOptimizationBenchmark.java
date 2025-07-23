package com.hft.memory.benchmark;

import com.hft.memory.core.*;
import com.hft.memory.pool.ObjectPool;
import com.hft.memory.memory.DirectMemoryManager;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class MemoryOptimizationBenchmark {
    
    // Test data
    private List<OrderData> testOrders;
    private MemoryOptimizedOrderProcessor optimizedProcessor;
    private TraditionalOrderProcessor traditionalProcessor;
    private ObjectPool<Order> objectPool;
    private DirectMemoryManager directMemoryManager;
    
    // Test configuration
    private static final int ORDER_COUNT = 10000;
    private static final String[] SYMBOLS = {"EURUSD", "GBPUSD", "USDJPY", "AUDUSD"};
    
    @Setup(Level.Trial)
    public void setupTrial() {
        // Initialize processors
        optimizedProcessor = new MemoryOptimizedOrderProcessor();
        traditionalProcessor = new TraditionalOrderProcessor();
        objectPool = new ObjectPool<>(Order::new, 1000);
        directMemoryManager = new DirectMemoryManager(1024 * 1024); // 1MB
        
        // Generate test data
        testOrders = generateTestOrders(ORDER_COUNT);
        
        System.out.println("=== Benchmark Setup Complete ===");
        System.out.println("Order count: " + ORDER_COUNT);
        System.out.println("Test data size: " + testOrders.size());
    }
    
    @Setup(Level.Iteration)
    public void setupIteration() {
        // Clear state before each iteration to ensure test consistency
        System.gc(); // Suggest GC run to reduce GC impact on tests
        try {
            Thread.sleep(100); // Wait for GC to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Benchmark 1: Object pool vs new operation
    @Benchmark
    public Order benchmarkObjectPoolAcquisition(Blackhole bh) {
        Order order = objectPool.acquire();
        bh.consume(order); // Prevent compiler optimization
        objectPool.release(order);
        return order;
    }
    
    @Benchmark
    public Order benchmarkNewObjectCreation(Blackhole bh) {
        Order order = new Order();
        bh.consume(order);
        return order;
    }
    
    // Benchmark 2: Direct memory vs heap memory serialization
    @Benchmark
    public boolean benchmarkDirectMemorySerialization() {
        OrderData data = testOrders.get(0);
        Order order = new Order(data.getId(), data.getSymbol(), 
                               data.getPrice(), data.getQuantity());
        
        return directMemoryManager.serializeOrder(order);
    }
    
    @Benchmark
    public byte[] benchmarkHeapMemorySerialization() throws Exception {
        OrderData data = testOrders.get(0);
        Order order = new Order(data.getId(), data.getSymbol(), 
                               data.getPrice(), data.getQuantity());
        
        // Simulate Java native serialization
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(order);
            return baos.toByteArray();
        }
    }
    
    // Benchmark 3: Primitive type collections vs Java Collections
    @Benchmark
    public void benchmarkTroveMap(Blackhole bh) {
        TIntObjectMap<Order> map = new TIntObjectHashMap<>();
        for (int i = 0; i < 1000; i++) {
            Order order = new Order();
            order.setId(i);
            map.put(i, order); // No boxing
            bh.consume(map.get(i));
        }
    }
    
    @Benchmark
    public void benchmarkHashMap(Blackhole bh) {
        Map<Integer, Order> map = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            Order order = new Order();
            order.setId(i);
            map.put(i, order); // Boxing overhead
            bh.consume(map.get(i));
        }
    }
    
    // Benchmark 4: Single order processing performance comparison
    @Benchmark
    public ProcessingResult benchmarkOptimizedOrderProcessing() {
        OrderData data = testOrders.get(0);
        return optimizedProcessor.processOrder(data);
    }
    
    @Benchmark
    public ProcessingResult benchmarkTraditionalOrderProcessing() {
        OrderData data = testOrders.get(0);
        return traditionalProcessor.processOrder(data);
    }
    
    // Benchmark 5: Batch order processing performance comparison
    @Benchmark
    public BatchProcessingResult benchmarkOptimizedBatchProcessing() {
        List<OrderData> batch = testOrders.subList(0, 100);
        return optimizedProcessor.processOrdersBatch(batch);
    }
    
    @Benchmark
    public BatchProcessingResult benchmarkTraditionalBatchProcessing() {
        List<OrderData> batch = testOrders.subList(0, 100);
        return traditionalProcessor.processOrdersBatch(batch);
    }
    
    // Benchmark 6: String operations optimization
    @Benchmark
    public String benchmarkOptimizedToString() {
        Order order = new Order(1, "EURUSD", 1.1234, 1000);
        return order.toString(); // Uses ThreadLocal StringBuilder
    }
    
    @Benchmark
    public String benchmarkTraditionalToString() {
        Order order = new Order(1, "EURUSD", 1.1234, 1000);
        // Simulate traditional toString
        return "Order{id=" + order.getId() + 
               ", symbol='" + order.getSymbol() + 
               "', price=" + order.getPrice() + 
               ", qty=" + order.getQuantity() + 
               ", side=" + (order.getSide() == 1 ? "BUY" : "SELL") + "}";
    }
    
    // Helper method: generate test data
    private List<OrderData> generateTestOrders(int count) {
        List<OrderData> orders = new ArrayList<>(count);
        Random random = new Random(42); // Fixed seed for reproducibility
        
        for (int i = 0; i < count; i++) {
            String symbol = SYMBOLS[random.nextInt(SYMBOLS.length)];
            double price = 1.0 + random.nextDouble();
            int quantity = 1000 + random.nextInt(9000);
            byte side = (byte) (random.nextBoolean() ? 1 : 2);
            
            orders.add(new OrderData(i, symbol, price, quantity, side));
        }
        
        return orders;
    }
    
    @TearDown(Level.Trial)
    public void tearDown() {
        optimizedProcessor.shutdown();
        traditionalProcessor.shutdown();
        
        // Print final statistics
        System.out.println("=== Benchmark Complete ===");
        System.out.println("Optimized processor stats: " + 
                          optimizedProcessor.getPerformanceStats());
        System.out.println("Traditional processor stats: " + 
                          traditionalProcessor.getPerformanceStats());
    }
    
    // Run benchmark
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(MemoryOptimizationBenchmark.class.getSimpleName())
                .shouldDoGC(true)
                .jvmArgs("-Xmx2g", "-Xms2g", 
                        "-XX:+UseG1GC", 
                        "-XX:MaxGCPauseMillis=10")
                .build();
        
        new Runner(opt).run();
    }
}