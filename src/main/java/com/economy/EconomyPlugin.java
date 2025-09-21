package com.economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyPlugin extends JavaPlugin implements Listener {
    
    private static EconomyPlugin instance;
    private EconomyManager economyManager;
    private MarketSystem marketSystem;
    private ShopGUI shopGUI;
    private SellGUI sellGUI;
    private Map<UUID, Double> playerBalances;
    
    @Override
    public void onEnable() {
        instance = this;
        playerBalances = new HashMap<>();
        
        // Load configuration
        saveDefaultConfig();
        
        // Initialize managers
        economyManager = new EconomyManager(this);
        marketSystem = new MarketSystem(this);
        shopGUI = new ShopGUI(this);
        sellGUI = new SellGUI(this);
        
        // Register commands
        registerCommands();
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(shopGUI, this);
        getServer().getPluginManager().registerEvents(sellGUI, this);
        
        // Start market price update task
        startMarketUpdateTask();
        
        // Load player data
        economyManager.loadAllPlayerData();
        
        getLogger().info("EconomyPlugin enabled! Market system active.");
    }
    
    @Override
    public void onDisable() {
        // Save all player data
        economyManager.saveAllPlayerData();
        
        getLogger().info("EconomyPlugin disabled! All data saved.");
    }
    
    private void registerCommands() {
        // Register command executors
        getCommand("balance").setExecutor(this);
        getCommand("shop").setExecutor(this);
        getCommand("sell").setExecutor(this);
    }
    
    private void startMarketUpdateTask() {
        int updateInterval = getConfig().getInt("market.update-interval", 5) * 60 * 20; // Convert minutes to ticks
        
        new BukkitRunnable() {
            @Override
            public void run() {
                marketSystem.updatePrices();
                getLogger().info("Market prices updated!");
            }
        }.runTaskTimer(this, updateInterval, updateInterval);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        
        Player player = (Player) sender;
        
        switch (command.getName().toLowerCase()) {
            case "balance":
            case "bal":
            case "money":
                if (player.hasPermission("economy.balance")) {
                    handleBalanceCommand(player);
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission to check your balance!");
                }
                break;
            case "shop":
            case "store":
            case "market":
                if (player.hasPermission("economy.shop")) {
                    shopGUI.openShop(player);
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use the shop!");
                }
                break;
            case "sell":
            case "s":
                if (player.hasPermission("economy.sell")) {
                    sellGUI.openSellMenu(player);
                } else {
                    player.sendMessage(ChatColor.RED + "You don't have permission to sell items!");
                }
                break;
        }
        
        return true;
    }
    
    private void handleBalanceCommand(Player player) {
        double balance = economyManager.getBalance(player.getUniqueId());
        String formattedBalance = String.format(getConfig().getString("currency.format", "%,.2f"), balance);
        String symbol = getConfig().getString("currency.symbol", "$");
        
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "╔══════════════════════════════════╗");
        player.sendMessage(ChatColor.GOLD + "║" + ChatColor.YELLOW + "           💰 YOUR BALANCE 💰           " + ChatColor.GOLD + "║");
        player.sendMessage(ChatColor.GOLD + "╠══════════════════════════════════╣");
        player.sendMessage(ChatColor.GOLD + "║" + ChatColor.WHITE + " Balance: " + ChatColor.GREEN + symbol + formattedBalance + ChatColor.GOLD + " ║");
        player.sendMessage(ChatColor.GOLD + "╠══════════════════════════════════╣");
        player.sendMessage(ChatColor.GOLD + "║" + ChatColor.GRAY + " Use /shop to buy items" + ChatColor.GOLD + " ║");
        player.sendMessage(ChatColor.GOLD + "║" + ChatColor.GRAY + " Use /sell to sell items" + ChatColor.GOLD + " ║");
        player.sendMessage(ChatColor.GOLD + "╚══════════════════════════════════╝");
        player.sendMessage("");
    }
    
    // Getters
    public static EconomyPlugin getInstance() {
        return instance;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public MarketSystem getMarketSystem() {
        return marketSystem;
    }
    
    public ShopGUI getShopGUI() {
        return shopGUI;
    }
    
    public SellGUI getSellGUI() {
        return sellGUI;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        economyManager.onPlayerJoin(player);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        economyManager.onPlayerQuit(player);
    }
}
