package com.economy;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MarketSystem {
    
    private final EconomyPlugin plugin;
    private final Map<Material, Double> basePrices;
    private final Map<Material, Double> currentPrices;
    private final Map<Material, Integer> supplyLevels;
    private final Map<Material, Integer> demandLevels;
    private final Random random;
    
    public MarketSystem(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.basePrices = new HashMap<>();
        this.currentPrices = new HashMap<>();
        this.supplyLevels = new HashMap<>();
        this.demandLevels = new HashMap<>();
        this.random = new Random();
        
        loadBasePrices();
        initializeMarket();
    }
    
    private void loadBasePrices() {
        ConfigurationSection priceSection = plugin.getConfig().getConfigurationSection("item-prices");
        if (priceSection != null) {
            for (String materialName : priceSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(materialName);
                    double price = priceSection.getDouble(materialName);
                    basePrices.put(material, price);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in config: " + materialName);
                }
            }
        }
    }
    
    private void initializeMarket() {
        // Initialize all materials with base prices and neutral supply/demand
        for (Map.Entry<Material, Double> entry : basePrices.entrySet()) {
            Material material = entry.getKey();
            double basePrice = entry.getValue();
            
            currentPrices.put(material, basePrice);
            supplyLevels.put(material, 0); // Neutral supply
            demandLevels.put(material, 0); // Neutral demand
        }
    }
    
    public void updatePrices() {
        double fluctuationRange = plugin.getConfig().getDouble("market.fluctuation-range", 0.15);
        double supplyDemandImpact = plugin.getConfig().getDouble("market.supply-demand-impact", 0.3);
        double minMultiplier = plugin.getConfig().getDouble("market.min-price-multiplier", 0.1);
        double maxMultiplier = plugin.getConfig().getDouble("market.max-price-multiplier", 5.0);
        
        for (Material material : basePrices.keySet()) {
            double basePrice = basePrices.get(material);
            double currentPrice = currentPrices.get(material);
            
            // Random fluctuation
            double randomChange = (random.nextDouble() - 0.5) * 2 * fluctuationRange;
            
            // Supply and demand impact
            int supply = supplyLevels.get(material);
            int demand = demandLevels.get(material);
            
            // Calculate supply/demand multiplier
            double supplyDemandMultiplier = 1.0;
            if (supply > demand) {
                // High supply, low demand = lower prices
                supplyDemandMultiplier -= (supply - demand) * supplyDemandImpact * 0.01;
            } else if (demand > supply) {
                // High demand, low supply = higher prices
                supplyDemandMultiplier += (demand - supply) * supplyDemandImpact * 0.01;
            }
            
            // Apply random fluctuation
            supplyDemandMultiplier += randomChange;
            
            // Clamp multiplier to prevent extreme prices
            supplyDemandMultiplier = Math.max(minMultiplier, Math.min(maxMultiplier, supplyDemandMultiplier));
            
            // Calculate new price
            double newPrice = basePrice * supplyDemandMultiplier;
            
            // Ensure minimum price from config
            double minPrice = plugin.getConfig().getDouble("market.min-price", 0.01);
            newPrice = Math.max(minPrice, newPrice);
            
            currentPrices.put(material, newPrice);
            
            // Gradually adjust supply and demand towards neutral
            adjustSupplyDemand(material);
        }
    }
    
    private void adjustSupplyDemand(Material material) {
        int currentSupply = supplyLevels.get(material);
        int currentDemand = demandLevels.get(material);
        
        // Use decay rate from config
        double decayRate = plugin.getConfig().getDouble("market.decay-rate", 0.8);
        
        // Gradually move towards neutral (0) using decay rate
        if (currentSupply > 0) {
            supplyLevels.put(material, Math.max(0, (int)(currentSupply * decayRate)));
        } else if (currentSupply < 0) {
            supplyLevels.put(material, Math.min(0, (int)(currentSupply * decayRate)));
        }
        
        if (currentDemand > 0) {
            demandLevels.put(material, Math.max(0, (int)(currentDemand * decayRate)));
        } else if (currentDemand < 0) {
            demandLevels.put(material, Math.min(0, (int)(currentDemand * decayRate)));
        }
    }
    
    public double getCurrentPrice(Material material) {
        return currentPrices.getOrDefault(material, 0.0);
    }
    
    public double getBasePrice(Material material) {
        return basePrices.getOrDefault(material, 0.0);
    }
    
    public void recordSale(Material material, int quantity) {
        // Increase supply when items are sold to the server
        int currentSupply = supplyLevels.getOrDefault(material, 0);
        supplyLevels.put(material, currentSupply + quantity);
    }
    
    public void recordPurchase(Material material, int quantity) {
        // Increase demand when items are bought from the server
        int currentDemand = demandLevels.getOrDefault(material, 0);
        demandLevels.put(material, currentDemand + quantity);
    }
    
    public String getPriceChangeIndicator(Material material) {
        double basePrice = getBasePrice(material);
        double currentPrice = getCurrentPrice(material);
        
        if (currentPrice > basePrice * 1.1) {
            return "&c↗"; // Rising
        } else if (currentPrice < basePrice * 0.9) {
            return "&a↘"; // Falling
        } else {
            return "&e→"; // Stable
        }
    }
    
    public String getSupplyDemandStatus(Material material) {
        int supply = supplyLevels.getOrDefault(material, 0);
        int demand = demandLevels.getOrDefault(material, 0);
        
        if (supply > demand + 10) {
            return "&aHigh Supply";
        } else if (demand > supply + 10) {
            return "&cHigh Demand";
        } else if (supply > demand) {
            return "&eSupply > Demand";
        } else if (demand > supply) {
            return "&eDemand > Supply";
        } else {
            return "&7Balanced";
        }
    }
    
    public Map<Material, Double> getAllCurrentPrices() {
        return new HashMap<>(currentPrices);
    }
    
    public Map<Material, Double> getAllBasePrices() {
        return new HashMap<>(basePrices);
    }
    
    public boolean isMaterialSupported(Material material) {
        return basePrices.containsKey(material);
    }
    
    public boolean isMaterialTradeable(Material material) {
        // Only allow trading of Building blocks, Ores, and Food
        return isMaterialSupported(material);
    }
    
    public void resetMarket() {
        // Reset all prices to base prices
        for (Map.Entry<Material, Double> entry : basePrices.entrySet()) {
            currentPrices.put(entry.getKey(), entry.getValue());
        }
        
        // Reset supply and demand
        for (Material material : basePrices.keySet()) {
            supplyLevels.put(material, 0);
            demandLevels.put(material, 0);
        }
    }
}
