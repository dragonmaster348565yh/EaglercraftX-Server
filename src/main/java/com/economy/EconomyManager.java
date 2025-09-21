package com.economy;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class EconomyManager {
    
    private final EconomyPlugin plugin;
    private final Map<UUID, Double> playerBalances;
    private final File dataFile;
    private FileConfiguration dataConfig;
    
    public EconomyManager(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.playerBalances = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        loadDataFile();
    }
    
    private void loadDataFile() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create data file", e);
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    public void loadAllPlayerData() {
        if (dataConfig.contains("balances")) {
            for (String uuidString : dataConfig.getConfigurationSection("balances").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    double balance = dataConfig.getDouble("balances." + uuidString);
                    playerBalances.put(uuid, balance);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in data file: " + uuidString);
                }
            }
        }
        plugin.getLogger().info("Loaded " + playerBalances.size() + " player balances");
    }
    
    public void saveAllPlayerData() {
        for (Map.Entry<UUID, Double> entry : playerBalances.entrySet()) {
            dataConfig.set("balances." + entry.getKey().toString(), entry.getValue());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data file", e);
        }
    }
    
    public void loadPlayerData(UUID uuid) {
        if (dataConfig.contains("balances." + uuid.toString())) {
            double balance = dataConfig.getDouble("balances." + uuid.toString());
            playerBalances.put(uuid, balance);
        } else {
            // Give new players starting balance
            double startingBalance = plugin.getConfig().getDouble("currency.starting-balance", 100.0);
            playerBalances.put(uuid, startingBalance);
            savePlayerData(uuid);
        }
    }
    
    public void savePlayerData(UUID uuid) {
        if (playerBalances.containsKey(uuid)) {
            dataConfig.set("balances." + uuid.toString(), playerBalances.get(uuid));
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save player data for " + uuid, e);
            }
        }
    }
    
    public double getBalance(UUID uuid) {
        if (!playerBalances.containsKey(uuid)) {
            loadPlayerData(uuid);
        }
        return playerBalances.getOrDefault(uuid, plugin.getConfig().getDouble("currency.starting-balance", 100.0));
    }
    
    public void setBalance(UUID uuid, double amount) {
        playerBalances.put(uuid, Math.max(0, amount));
        savePlayerData(uuid);
    }
    
    public boolean deposit(UUID uuid, double amount) {
        if (amount <= 0) return false;
        
        double currentBalance = getBalance(uuid);
        setBalance(uuid, currentBalance + amount);
        return true;
    }
    
    public boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) return false;
        
        double currentBalance = getBalance(uuid);
        if (currentBalance < amount) return false;
        
        setBalance(uuid, currentBalance - amount);
        return true;
    }
    
    public boolean hasEnough(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }
    
    public void resetBalance(UUID uuid) {
        double startingBalance = plugin.getConfig().getDouble("currency.starting-balance", 100.0);
        setBalance(uuid, startingBalance);
    }
    
    public Map<UUID, Double> getAllBalances() {
        return new HashMap<>(playerBalances);
    }
    
    public String formatBalance(double amount) {
        String format = plugin.getConfig().getString("currency.format", "%,.2f");
        String symbol = plugin.getConfig().getString("currency.symbol", "$");
        return symbol + String.format(format, amount);
    }
    
    public void onPlayerJoin(Player player) {
        loadPlayerData(player.getUniqueId());
    }
    
    public void onPlayerQuit(Player player) {
        savePlayerData(player.getUniqueId());
    }
}
