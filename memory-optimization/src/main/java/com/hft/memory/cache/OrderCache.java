package com.hft.memory.cache;

import com.hft.memory.core.Order;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderCache {
    // Use primitive type Map to avoid Integer boxing
    private final TIntObjectMap<Order> orderById;
    private final Map<String, TIntList> ordersBySymbol;
    
    // Pre-allocate capacity to reduce rehashing
    private static final int INITIAL_CAPACITY = 100000;
    private static final float LOAD_FACTOR = 0.75f;
    
    public OrderCache() {
        this.orderById = new TIntObjectHashMap<>(INITIAL_CAPACITY, LOAD_FACTOR);
        this.ordersBySymbol = new ConcurrentHashMap<>(100); // Assume 100 trading symbols
    }
    
    // High-performance order addition
    public void addOrder(Order order) {
        // Primitive type operation, no boxing overhead
        orderById.put(order.getId(), order);
        
        // Group by symbol
        String symbol = order.getSymbol();
        ordersBySymbol.computeIfAbsent(symbol, 
            k -> new TIntArrayList()).add(order.getId());
    }
    
    // High-performance lookup
    public Order getOrder(int orderId) {
        return orderById.get(orderId); // No boxing
    }
    
    // Find orders by symbol
    public List<Order> getOrdersBySymbol(String symbol) {
        TIntList orderIds = ordersBySymbol.get(symbol);
        if (orderIds == null) {
            return Collections.emptyList();
        }
        
        List<Order> orders = new ArrayList<>(orderIds.size());
        orderIds.forEach(orderId -> {
            Order order = orderById.get(orderId);
            if (order != null) {
                orders.add(order);
            }
            return true; // Continue iteration
        });
        
        return orders;
    }
    
    // High-performance order removal
    public Order removeOrder(int orderId) {
        Order order = orderById.remove(orderId);
        if (order != null) {
            String symbol = order.getSymbol();
            TIntList symbolOrders = ordersBySymbol.get(symbol);
            if (symbolOrders != null) {
                symbolOrders.remove(orderId);
                if (symbolOrders.isEmpty()) {
                    ordersBySymbol.remove(symbol);
                }
            }
        }
        return order;
    }
    
    // Update existing order
    public boolean updateOrder(Order order) {
        int orderId = order.getId();
        Order existingOrder = orderById.get(orderId);
        if (existingOrder != null) {
            // Remove from old symbol group if symbol changed
            String oldSymbol = existingOrder.getSymbol();
            String newSymbol = order.getSymbol();
            
            if (!oldSymbol.equals(newSymbol)) {
                TIntList oldSymbolOrders = ordersBySymbol.get(oldSymbol);
                if (oldSymbolOrders != null) {
                    oldSymbolOrders.remove(orderId);
                    if (oldSymbolOrders.isEmpty()) {
                        ordersBySymbol.remove(oldSymbol);
                    }
                }
                
                // Add to new symbol group
                ordersBySymbol.computeIfAbsent(newSymbol, 
                    k -> new TIntArrayList()).add(orderId);
            }
            
            // Update the order
            orderById.put(orderId, order);
            return true;
        }
        return false;
    }
    
    // Cache statistics
    public void printStats() {
        System.out.printf("Total orders: %d%n", orderById.size());
        System.out.printf("Symbols: %d%n", ordersBySymbol.size());
        
        // Memory usage estimation
        long memoryUsed = orderById.size() * 64; // Estimate 64 bytes per order
        System.out.printf("Estimated memory: %.2f MB%n", memoryUsed / 1024.0 / 1024.0);
        
        // Symbol distribution
        ordersBySymbol.forEach((symbol, orders) -> 
            System.out.printf("Symbol %s: %d orders%n", symbol, orders.size()));
    }
    
    public int size() {
        return orderById.size();
    }
    
    public int getSymbolCount() {
        return ordersBySymbol.size();
    }
    
    public Set<String> getSymbols() {
        return ordersBySymbol.keySet();
    }
    
    public void clear() {
        orderById.clear();
        ordersBySymbol.clear();
    }
    
    // Get all orders as collection
    public Collection<Order> getAllOrders() {
        return orderById.valueCollection();
    }
    
    // Batch operations
    public void addOrdersBatch(List<Order> orders) {
        for (Order order : orders) {
            addOrder(order);
        }
    }
    
    public List<Order> removeOrdersBatch(int[] orderIds) {
        List<Order> removedOrders = new ArrayList<>(orderIds.length);
        for (int orderId : orderIds) {
            Order removed = removeOrder(orderId);
            if (removed != null) {
                removedOrders.add(removed);
            }
        }
        return removedOrders;
    }
}