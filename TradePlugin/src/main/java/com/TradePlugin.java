package com.example.tradeplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;

public class TradePlugin extends JavaPlugin implements CommandExecutor {
    private TradeManager tradeManager;
    
    @Override
    public void onEnable() {
        tradeManager = new TradeManager(this);
        new TradeGUI(this); // Register listener
        
        this.getCommand("trade").setExecutor(this);
        this.getCommand("ta").setExecutor(this);
        this.getCommand("td").setExecutor(this);
        getLogger().info("TradePlugin enabled!");
    }
    
    @Override
    public void onDisable() {
        // Cancel all active trades when plugin disables
        tradeManager.cancelAllTrades();
        getLogger().info("TradePlugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (command.getName().equalsIgnoreCase("trade")) {
            if (args.length != 1) {
                player.sendMessage(ChatColor.YELLOW + "Usage: /trade <player>");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "Player not found or not online.");
                return true;
            }
            
            if (player.equals(target)) {
                player.sendMessage(ChatColor.RED + "You cannot trade with yourself.");
                return true;
            }
            
            if (tradeManager.isPlayerTrading(player)) {
                player.sendMessage(ChatColor.RED + "You are already in a trade!");
                return true;
            }
            
            if (tradeManager.isPlayerTrading(target)) {
                player.sendMessage(ChatColor.RED + target.getName() + " is already in a trade!");
                return true;
            }
            
            tradeManager.sendTradeRequest(player, target);
            return true;
        }
        else if (command.getName().equalsIgnoreCase("ta")) {
            tradeManager.acceptTradeRequest(player);
            return true;
        }
        else if (command.getName().equalsIgnoreCase("td")) {
            tradeManager.declineTradeRequest(player);
            return true;
        }
        
        return false;
    }
    
    public TradeManager getTradeManager() {
        return tradeManager;
    }
}