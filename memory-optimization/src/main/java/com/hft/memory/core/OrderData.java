package com.hft.memory.core;

public class OrderData {
    private final int id;
    private final String symbol;
    private final double price;
    private final int quantity;
    private final byte side;
    
    public OrderData(int id, String symbol, double price, int quantity, byte side) {
        this.id = id;
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.side = side;
    }
    
    public int getId() { return id; }
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public byte getSide() { return side; }
    
    @Override
    public String toString() {
        return String.format("OrderData{id=%d, symbol='%s', price=%.4f, qty=%d, side=%s}",
            id, symbol, price, quantity, side == 1 ? "BUY" : "SELL");
    }
}