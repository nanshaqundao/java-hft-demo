package com.hft.lockfree.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 价格数据生成器工具类
 * 
 * 用于生成测试用的价格数据，模拟真实的市场价格行为
 */
public class PriceDataGenerator {
    
    // 预定义的交易品种及其基础价格
    private static final SymbolConfig[] SYMBOL_CONFIGS = {
        new SymbolConfig("EURUSD", 1.1000, 0.0001, 0.001),
        new SymbolConfig("GBPUSD", 1.3000, 0.0001, 0.002),
        new SymbolConfig("USDJPY", 110.00, 0.01, 0.1),
        new SymbolConfig("AUDUSD", 0.7500, 0.0001, 0.0015),
        new SymbolConfig("USDCAD", 1.2500, 0.0001, 0.0012),
        new SymbolConfig("EURGBP", 0.8500, 0.0001, 0.0008),
        new SymbolConfig("EURJPY", 130.00, 0.01, 0.15),
        new SymbolConfig("GBPJPY", 150.00, 0.01, 0.2),
        new SymbolConfig("XAUUSD", 1800.0, 0.01, 5.0),
        new SymbolConfig("USOIL", 70.00, 0.01, 2.0)
    };
    
    /**
     * 获取所有可用的交易品种
     */
    public static String[] getAllSymbols() {
        String[] symbols = new String[SYMBOL_CONFIGS.length];
        for (int i = 0; i < SYMBOL_CONFIGS.length; i++) {
            symbols[i] = SYMBOL_CONFIGS[i].symbol;
        }
        return symbols;
    }
    
    /**
     * 获取指定交易品种的配置
     */
    public static SymbolConfig getSymbolConfig(String symbol) {
        for (SymbolConfig config : SYMBOL_CONFIGS) {
            if (config.symbol.equals(symbol)) {
                return config;
            }
        }
        throw new IllegalArgumentException("Unknown symbol: " + symbol);
    }
    
    /**
     * 生成随机价格
     * @param symbol 交易品种
     * @return 价格对象 {bidPrice, askPrice}
     */
    public static PricePair generateRandomPrice(String symbol) {
        SymbolConfig config = getSymbolConfig(symbol);
        
        // 生成随机波动
        double volatility = ThreadLocalRandom.current().nextGaussian() * config.volatility;
        double midPrice = config.basePrice + volatility;
        
        // 生成随机点差
        double spread = config.minSpread + ThreadLocalRandom.current().nextDouble() * config.minSpread * 2;
        
        double bidPrice = midPrice - spread / 2;
        double askPrice = midPrice + spread / 2;
        
        return new PricePair(bidPrice, askPrice);
    }
    
    /**
     * 生成趋势价格（带方向性）
     * @param symbol 交易品种
     * @param trend 趋势方向 (-1.0 到 1.0)
     * @return 价格对象
     */
    public static PricePair generateTrendingPrice(String symbol, double trend) {
        SymbolConfig config = getSymbolConfig(symbol);
        
        // 趋势性波动
        double trendComponent = trend * config.volatility * 0.5;
        double randomComponent = ThreadLocalRandom.current().nextGaussian() * config.volatility * 0.5;
        
        double midPrice = config.basePrice + trendComponent + randomComponent;
        double spread = config.minSpread + ThreadLocalRandom.current().nextDouble() * config.minSpread;
        
        double bidPrice = midPrice - spread / 2;
        double askPrice = midPrice + spread / 2;
        
        return new PricePair(bidPrice, askPrice);
    }
    
    /**
     * 生成批量价格数据
     * @param symbol 交易品种
     * @param count 数量
     * @return 价格数组
     */
    public static PricePair[] generateBatchPrices(String symbol, int count) {
        PricePair[] prices = new PricePair[count];
        for (int i = 0; i < count; i++) {
            prices[i] = generateRandomPrice(symbol);
        }
        return prices;
    }
    
    /**
     * 交易品种配置
     */
    public static class SymbolConfig {
        public final String symbol;
        public final double basePrice;
        public final double minSpread;
        public final double volatility;
        
        public SymbolConfig(String symbol, double basePrice, double minSpread, double volatility) {
            this.symbol = symbol;
            this.basePrice = basePrice;
            this.minSpread = minSpread;
            this.volatility = volatility;
        }
        
        @Override
        public String toString() {
            return String.format("SymbolConfig{symbol='%s', basePrice=%.5f, minSpread=%.5f, volatility=%.5f}", 
                    symbol, basePrice, minSpread, volatility);
        }
    }
    
    /**
     * 价格对
     */
    public static class PricePair {
        public final double bidPrice;
        public final double askPrice;
        
        public PricePair(double bidPrice, double askPrice) {
            this.bidPrice = bidPrice;
            this.askPrice = askPrice;
        }
        
        public double getSpread() {
            return askPrice - bidPrice;
        }
        
        public double getMidPrice() {
            return (bidPrice + askPrice) / 2.0;
        }
        
        @Override
        public String toString() {
            return String.format("PricePair{bid=%.5f, ask=%.5f, spread=%.5f}", 
                    bidPrice, askPrice, getSpread());
        }
    }
}