package com.example.tradeplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeManager {
    private TradePlugin plugin;
    private Map<UUID, TradeInvite> pendingInvites;
    private Map<UUID, UUID> activeTrades;
    
    public TradeManager(TradePlugin plugin) {
        this.plugin = plugin;
        this.pendingInvites = new HashMap<>();
        this.activeTrades = new HashMap<>();
    }
    
    public void sendTradeRequest(Player sender, Player target) {
        // Cancel any existing invite
        if (pendingInvites.containsKey(target.getUniqueId())) {
            TradeInvite existingInvite = pendingInvites.get(target.getUniqueId());
            if (existingInvite.getSender().equals(sender)) {
                sender.sendMessage(ChatColor.YELLOW + "You already sent a trade request to " + target.getName() + ".");
                return;
            }
        }
        
        TradeInvite invite = new TradeInvite(sender, target);
        pendingInvites.put(target.getUniqueId(), invite);
        
        sender.sendMessage(ChatColor.GREEN + "Trade request sent to " + target.getName() + ".");
        target.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "✉ Trade Request");
        target.sendMessage(ChatColor.AQUA + sender.getName() + " wants to trade with you!");
        target.sendMessage(ChatColor.GREEN + "Type " + ChatColor.BOLD + "/ta" + ChatColor.RESET + ChatColor.GREEN + " to accept");
        target.sendMessage(ChatColor.RED + "Type " + ChatColor.BOLD + "/td" + ChatColor.RESET + ChatColor.RED + " to decline");
        target.sendMessage(ChatColor.GRAY + "This request will expire in 30 seconds.");
        
        // Set expiration timer
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingInvites.containsKey(target.getUniqueId()) && 
                    pendingInvites.get(target.getUniqueId()).equals(invite)) {
                    pendingInvites.remove(target.getUniqueId());
                    sender.sendMessage(ChatColor.RED + "Your trade request to " + target.getName() + " has expired.");
                    target.sendMessage(ChatColor.RED + "Trade request from " + sender.getName() + " has expired.");
                }
            }
        }.runTaskLater(plugin, 30 * 20); // 30 seconds
    }
    
    public void acceptTradeRequest(Player player) {
        if (!pendingInvites.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You don't have any pending trade requests.");
            return;
        }
        
        TradeInvite invite = pendingInvites.get(player.getUniqueId());
        Player sender = invite.getSender();
        
        if (!sender.isOnline()) {
            player.sendMessage(ChatColor.RED + "The player who sent the trade request is no longer online.");
            pendingInvites.remove(player.getUniqueId());
            return;
        }
        
        pendingInvites.remove(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "You accepted the trade request from " + sender.getName() + ".");
        sender.sendMessage(ChatColor.GREEN + player.getName() + " accepted your trade request!");
        
        // Open trade GUI for both players
        TradeGUI.openTradeGUI(sender, player);
        activeTrades.put(sender.getUniqueId(), player.getUniqueId());
        activeTrades.put(player.getUniqueId(), sender.getUniqueId());
    }
    
    public void declineTradeRequest(Player player) {
        if (!pendingInvites.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You don't have any pending trade requests.");
            return;
        }
        
        TradeInvite invite = pendingInvites.get(player.getUniqueId());
        Player sender = invite.getSender();
        
        pendingInvites.remove(player.getUniqueId());
        player.sendMessage(ChatColor.RED + "You declined the trade request from " + sender.getName() + ".");
        
        if (sender.isOnline()) {
            sender.sendMessage(ChatColor.RED + player.getName() + " declined your trade request.");
        }
    }
    
    public void cancelAllInvites(Player player) {
        // Remove any invites where this player is the target
        pendingInvites.remove(player.getUniqueId());
        
        // Remove any invites where this player is the sender
        for (UUID targetId : pendingInvites.keySet()) {
            TradeInvite invite = pendingInvites.get(targetId);
            if (invite.getSender().equals(player)) {
                Player target = Bukkit.getPlayer(targetId);
                if (target != null && target.isOnline()) {
                    target.sendMessage(ChatColor.RED + player.getName() + " canceled the trade request.");
                }
                pendingInvites.remove(targetId);
                break;
            }
        }
    }
    
    public boolean isPlayerTrading(Player player) {
        return activeTrades.containsKey(player.getUniqueId());
    }
    
    public void removeActiveTrade(Player player) {
        UUID partnerId = activeTrades.get(player.getUniqueId());
        if (partnerId != null) {
            activeTrades.remove(partnerId);
        }
        activeTrades.remove(player.getUniqueId());
    }
    
    public void cancelAllTrades() {
        // Return items to all players in active trades
        for (UUID playerId : activeTrades.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                TradeGUI.returnItemsToPlayer(player);
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "Trade was cancelled due to server reload.");
            }
        }
        activeTrades.clear();
    }
    
    public boolean hasEnoughSpace(Player player, ItemStack[] items) {
        // Create a copy of the player's inventory to simulate adding items
        ItemStack[] inventoryCopy = player.getInventory().getContents().clone();
        
        for (ItemStack item : items) {
            if (item == null) continue;
            
            boolean added = false;
            // Try to add to existing stacks first
            for (int i = 0; i < inventoryCopy.length; i++) {
                if (inventoryCopy[i] != null && inventoryCopy[i].isSimilar(item)) {
                    int maxStackSize = item.getMaxStackSize();
                    int currentAmount = inventoryCopy[i].getAmount();
                    int toAdd = item.getAmount();
                    
                    if (currentAmount + toAdd <= maxStackSize) {
                        inventoryCopy[i].setAmount(currentAmount + toAdd);
                        added = true;
                        break;
                    } else {
                        int canAdd = maxStackSize - currentAmount;
                        toAdd -= canAdd;
                        inventoryCopy[i].setAmount(maxStackSize);
                        item.setAmount(toAdd);
                    }
                }
            }
            
            // If not all items were added to existing stacks, try empty slots
            if (!added) {
                for (int i = 0; i < inventoryCopy.length; i++) {
                    if (inventoryCopy[i] == null) {
                        inventoryCopy[i] = item;
                        added = true;
                        break;
                    }
                }
            }
            
            if (!added) {
                return false; // Not enough space
            }
        }
        
        return true;
    }
    
    public void completeTrade(Player player1, Player player2, ItemStack[] player1Items, ItemStack[] player2Items) {
        // Check if players have enough inventory space
        if (!hasEnoughSpace(player1, player2Items)) {
            player1.sendMessage(ChatColor.RED + "Trade failed: You don't have enough space in your inventory!");
            player2.sendMessage(ChatColor.RED + "Trade failed: " + player1.getName() + " doesn't have enough space!");
            TradeGUI.closeTradeForBothPlayers(player1);
            return;
        }
        
        if (!hasEnoughSpace(player2, player1Items)) {
            player2.sendMessage(ChatColor.RED + "Trade failed: You don't have enough space in your inventory!");
            player1.sendMessage(ChatColor.RED + "Trade failed: " + player2.getName() + " doesn't have enough space!");
            TradeGUI.closeTradeForBothPlayers(player1);
            return;
        }
        
        // Remove from active trades
        removeActiveTrade(player1);
        removeActiveTrade(player2);
        
        // Give items to players
        giveItemsToPlayer(player1, player2Items);
        giveItemsToPlayer(player2, player1Items);
        
        player1.sendMessage(ChatColor.GREEN + "Trade completed successfully!");
        player2.sendMessage(ChatColor.GREEN + "Trade completed successfully!");
    }
    
    private void giveItemsToPlayer(Player player, ItemStack[] items) {
        for (ItemStack item : items) {
            if (item != null) {
                HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
                // This shouldn't happen because we checked space, but just in case
                for (ItemStack leftItem : leftOver.values()) {
                    player.getWorld().dropItem(player.getLocation(), leftItem);
                    player.sendMessage(ChatColor.YELLOW + "Some items were dropped on the ground due to lack of space.");
                }
            }
        }
    }
}