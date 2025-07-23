package com.hft.memory.benchmark;

import com.hft.memory.core.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * JMH Benchmarks comparing Java 21 features with traditional approaches
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class Java21FeaturesBenchmark {
    
    private List<OrderData> testOrders;
    private VirtualThreadOrderProcessor virtualThreadProcessor;
    private MemoryOptimizedOrderProcessor traditionalProcessor;
    
    private static final int ORDER_COUNT = 1000;
    private static final String[] SYMBOLS = {"EURUSD", "GBPUSD", "USDJPY", "AUDUSD"};
    
    @Setup(Level.Trial)
    public void setupTrial() {
        virtualThreadProcessor = new VirtualThreadOrderProcessor();
        traditionalProcessor = new MemoryOptimizedOrderProcessor();
        testOrders = generateTestOrders(ORDER_COUNT);
        
        System.out.println("=== Java 21 Features Benchmark Setup ===");
        System.out.println("Order count: " + ORDER_COUNT);
        System.out.println("Virtual Threads available: true");
    }
    
    // Benchmark 1: Record vs Class creation
    @Benchmark
    public OrderRecord benchmarkRecordCreation() {
        OrderData data = testOrders.get(0);
        return OrderRecord.create(data.getId(), data.getSymbol(), 
            data.getPrice(), data.getQuantity(), data.getSide());
    }
    
    @Benchmark
    public Order benchmarkClassCreation() {
        OrderData data = testOrders.get(0);
        return new Order(data.getId(), data.getSymbol(), 
            data.getPrice(), data.getQuantity());
    }
    
    // Benchmark 2: Virtual Thread vs Traditional Thread processing
    @Benchmark
    public List<ProcessingResult> benchmarkVirtualThreadProcessing() {
        List<OrderData> batch = testOrders.subList(0, 100);
        return virtualThreadProcessor.processOrdersConcurrently(batch).join();
    }
    
    @Benchmark  
    public BatchProcessingResult benchmarkTraditionalProcessing() {
        List<OrderData> batch = testOrders.subList(0, 100);
        return traditionalProcessor.processOrdersBatch(batch);
    }
    
    // Benchmark 3: String formatting - formatted() vs String.format()
    @Benchmark
    public String benchmarkFormattedMethod() {
        OrderData data = testOrders.get(0);
        return "Order{id=%d, symbol='%s', price=%.4f, qty=%d}"
            .formatted(data.getId(), data.getSymbol(), data.getPrice(), data.getQuantity());
    }
    
    @Benchmark
    public String benchmarkStringFormat() {
        OrderData data = testOrders.get(0);
        return String.format("Order{id=%d, symbol='%s', price=%.4f, qty=%d}",
            data.getId(), data.getSymbol(), data.getPrice(), data.getQuantity());
    }
    
    // Benchmark 4: Pattern matching with switch expressions
    @Benchmark
    public String benchmarkPatternMatchingSwitch(Blackhole bh) {
        ProcessingResult result = new ProcessingResult(true, 1000L, true);
        String formatted = virtualThreadProcessor.formatOrderResult(result);
        bh.consume(formatted);
        return formatted;
    }
    
    @Benchmark
    public String benchmarkTraditionalIfElse(Blackhole bh) {
        ProcessingResult result = new ProcessingResult(true, 1000L, true);
        String formatted;
        if (result.isSuccess() && result.isSerialized()) {
            formatted = String.format("SUCCESS: Processed and serialized in %.2fμs", 
                result.getProcessingTimeNs() / 1000.0);
        } else if (result.isSuccess() && !result.isSerialized()) {
            formatted = String.format("PARTIAL: Processed but not serialized in %.2fμs", 
                result.getProcessingTimeNs() / 1000.0);
        } else {
            formatted = String.format("FAILED: %s after %.2fμs",
                result.getError() != null ? result.getError().getMessage() : "Unknown error",
                result.getProcessingTimeNs() / 1000.0);
        }
        bh.consume(formatted);
        return formatted;
    }
    
    // Benchmark 5: Stream API with pattern matching
    @Benchmark
    public long benchmarkStreamWithPatternMatching() {
        return testOrders.stream()
            .filter(order -> switch (order.getSide()) {
                case 1 -> order.getPrice() > 1.0; // BUY orders above 1.0
                case 2 -> order.getPrice() < 2.0; // SELL orders below 2.0  
                default -> false;
            })
            .count();
    }
    
    @Benchmark
    public long benchmarkStreamWithTraditionalFilter() {
        return testOrders.stream()
            .filter(order -> {
                if (order.getSide() == 1) {
                    return order.getPrice() > 1.0;
                } else if (order.getSide() == 2) {
                    return order.getPrice() < 2.0;
                } else {
                    return false;
                }
            })
            .count();
    }
    
    // Benchmark 6: Record equals/hashCode vs manual implementation
    @Benchmark
    public boolean benchmarkRecordEquals() {
        OrderRecord r1 = OrderRecord.create(1, "EURUSD", 1.1234, 1000, (byte)1);
        OrderRecord r2 = OrderRecord.create(1, "EURUSD", 1.1234, 1000, (byte)1);
        return r1.equals(r2);
    }
    
    @Benchmark
    public boolean benchmarkManualEquals() {
        Order o1 = new Order(1, "EURUSD", 1.1234, 1000);
        Order o2 = new Order(1, "EURUSD", 1.1234, 1000);
        return o1.equals(o2);
    }
    
    // Benchmark 7: Virtual thread creation overhead
    @Benchmark
    @Measurement(iterations = 3, time = 5)
    public void benchmarkVirtualThreadCreation(Blackhole bh) {
        Thread vt = Thread.ofVirtual().start(() -> {
            // Minimal work
            bh.consume(System.nanoTime());
        });
        try {
            vt.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Benchmark
    @Measurement(iterations = 3, time = 5)
    public void benchmarkPlatformThreadCreation(Blackhole bh) {
        Thread pt = Thread.ofPlatform().start(() -> {
            // Minimal work
            bh.consume(System.nanoTime());
        });
        try {
            pt.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private List<OrderData> generateTestOrders(int count) {
        List<OrderData> orders = new ArrayList<>(count);
        Random random = new Random(42);
        
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
        virtualThreadProcessor.shutdown();
        traditionalProcessor.shutdown();
        
        System.out.println("=== Java 21 Features Benchmark Complete ===");
    }
    
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(Java21FeaturesBenchmark.class.getSimpleName())
            .shouldDoGC(true)
            .warmupIterations(1)
            .measurementIterations(2)
            .forks(0) // No forking - run in same JVM
            .build();
        
        new Runner(opt).run();
    }
}