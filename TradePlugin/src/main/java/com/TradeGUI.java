package com.example.tradeplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeGUI implements Listener {
    private static Map<UUID, Inventory> tradeInventories = new HashMap<>();
    private static Map<UUID, UUID> tradePartners = new HashMap<>();
    private static Map<UUID, Boolean> playerAccepted = new HashMap<>();
    private static Map<UUID, ItemStack[]> offeredItems = new HashMap<>();
    
    private TradePlugin plugin;
    
    public TradeGUI(Plugin plugin) {
        this.plugin = (TradePlugin) plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public static void openTradeGUI(Player player1, Player player2) {
        // Create separate inventories for each player
        Inventory inv1 = createTradeInventory(player1, player2);
        Inventory inv2 = createTradeInventory(player2, player1);
        
        // Open inventory for both players
        player1.openInventory(inv1);
        player2.openInventory(inv2);
        
        // Store references
        tradeInventories.put(player1.getUniqueId(), inv1);
        tradeInventories.put(player2.getUniqueId(), inv2);
        tradePartners.put(player1.getUniqueId(), player2.getUniqueId());
        tradePartners.put(player2.getUniqueId(), player1.getUniqueId());
        
        // Initialize acceptance status and offered items
        playerAccepted.put(player1.getUniqueId(), false);
        playerAccepted.put(player2.getUniqueId(), false);
        offeredItems.put(player1.getUniqueId(), new ItemStack[4]);
        offeredItems.put(player2.getUniqueId(), new ItemStack[4]);
    }
    
    private static Inventory createTradeInventory(Player player, Player partner) {
        // Create inventory with 27 slots (3 rows)
        Inventory inv = Bukkit.createInventory(null, 27, 
            ChatColor.BLUE + "Trading with: " + partner.getName());
        
        // Setup layout
        setupTradeLayout(inv, player, partner);
        
        return inv;
    }
    
    private static void setupTradeLayout(Inventory inv, Player player, Player partner) {
        // Clear inventory
        inv.clear();
        
        // Add separator in the middle (gray stained glass)
        ItemStack separator = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 7);
        ItemMeta sepMeta = separator.getItemMeta();
        sepMeta.setDisplayName(ChatColor.GRAY + "↔ Trade Separator");
        separator.setItemMeta(sepMeta);
        
        // Middle column separator
        for (int i = 0; i < 3; i++) {
            inv.setItem(i * 9 + 4, separator);
        }
        
        // Add player names
        ItemStack playerHead = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta playerMeta = playerHead.getItemMeta();
        playerMeta.setDisplayName(ChatColor.GREEN + "Your Items");
        playerHead.setItemMeta(playerMeta);
        inv.setItem(0, playerHead);
        
        ItemStack partnerHead = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        ItemMeta partnerMeta = partnerHead.getItemMeta();
        partnerMeta.setDisplayName(ChatColor.GREEN + partner.getName() + "'s Items");
        partnerHead.setItemMeta(partnerMeta);
        inv.setItem(8, partnerHead);
        
        // Add accept button (green wool) - ONLY on player's side
        ItemStack acceptBtn = new ItemStack(Material.WOOL, 1, (short) 5);
        ItemMeta acceptMeta = acceptBtn.getItemMeta();
        acceptMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "ACCEPT TRADE");
        acceptMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Click to accept the trade",
            ChatColor.GRAY + "Both players must accept to complete"
        ));
        acceptBtn.setItemMeta(acceptMeta);
        inv.setItem(22, acceptBtn);
        
        // Add decline button (red wool) - ONLY on player's side
        ItemStack declineBtn = new ItemStack(Material.WOOL, 1, (short) 14);
        ItemMeta declineMeta = declineBtn.getItemMeta();
        declineMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "DECLINE TRADE");
        declineMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Click to decline the trade",
            ChatColor.GRAY + "This will cancel the trade for both players"
        ));
        declineBtn.setItemMeta(declineMeta);
        inv.setItem(24, declineBtn);
        
        // Add status indicators
        ItemStack yourStatus = new ItemStack(Material.WOOL, 1, (short) 14);
        ItemMeta yourStatusMeta = yourStatus.getItemMeta();
        yourStatusMeta.setDisplayName(ChatColor.RED + "You: NOT READY");
        yourStatus.setItemMeta(yourStatusMeta);
        inv.setItem(18, yourStatus);
        
        ItemStack partnerStatus = new ItemStack(Material.WOOL, 1, (short) 14);
        ItemMeta partnerStatusMeta = partnerStatus.getItemMeta();
        partnerStatusMeta.setDisplayName(ChatColor.RED + partner.getName() + ": NOT READY");
        partnerStatus.setItemMeta(partnerStatusMeta);
        inv.setItem(26, partnerStatus);
        
        // Add trading areas (empty slots)
        // Player's area (slots 10-12, 19-21)
        // Partner's area is simulated and not interactable
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (j < 2) { // Only first two columns for player's items
                    inv.setItem(10 + i * 9 + j, new ItemStack(Material.AIR));
                }
                // Partner's side (right side) is filled with gray glass - not interactable
                inv.setItem(14 + i * 9 + j, new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 8));
            }
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        
        // Check if this is a trade inventory
        if (!tradeInventories.containsKey(player.getUniqueId()) || 
            !tradeInventories.get(player.getUniqueId()).equals(inv)) return;
        
        String title = inv.getTitle();
        if (!title.startsWith(ChatColor.BLUE + "Trading with: ")) return;
        
        int slot = event.getRawSlot();
        
        // Prevent clicking on control items, partner's side, and separator
        if (slot == 0 || slot == 8 || slot == 18 || slot == 22 || slot == 24 || slot == 26 ||
            slot == 4 || slot == 13 || slot == 22 || // Separator column
            slot >= 14 && slot <= 16 || // Partner's area row 1
            slot >= 23 && slot <= 25 || // Partner's area row 2
            slot >= 32 && slot <= 34) { // Partner's area row 3
            event.setCancelled(true);
            
            // Handle accept button
            if (slot == 22) {
                handleAcceptTrade(player);
            }
            // Handle decline button
            else if (slot == 24) {
                player.sendMessage(ChatColor.RED + "You declined the trade.");
                closeTradeForBothPlayers(player);
            }
            return;
        }
        
        // Allow item placement only in the player's trading area
        // Player's area: slots 10-12, 19-21, 28-30 (first two columns of each row)
        boolean isPlayerArea = 
            (slot >= 10 && slot <= 12) || 
            (slot >= 19 && slot <= 21) || 
            (slot >= 28 && slot <= 30);
        
        if (!isPlayerArea) {
            event.setCancelled(true);
            return;
        }
        
        // Update acceptance status if items changed
        playerAccepted.put(player.getUniqueId(), false);
        updateAcceptStatus(player);
    }
    
    private void handleAcceptTrade(Player player) {
        UUID partnerId = tradePartners.get(player.getUniqueId());
        if (partnerId == null) return;
        
        Player partner = Bukkit.getPlayer(partnerId);
        if (partner == null) {
            closeTradeForBothPlayers(player);
            return;
        }
        
        // Toggle acceptance
        playerAccepted.put(player.getUniqueId(), !playerAccepted.get(player.getUniqueId()));
        
        // Update UI
        updateAcceptStatus(player);
        
        // Check if both players accepted
        boolean bothAccepted = playerAccepted.get(player.getUniqueId()) && 
                              playerAccepted.get(partnerId);
        
        if (bothAccepted) {
            completeTrade(player, partner);
        } else {
            player.sendMessage(ChatColor.YELLOW + "Trade " + 
                (playerAccepted.get(player.getUniqueId()) ? "accepted" : "unaccepted") + 
                ". Waiting for " + partner.getName() + " to accept.");
            
            if (playerAccepted.get(player.getUniqueId())) {
                partner.sendMessage(ChatColor.GREEN + player.getName() + " has accepted the trade!");
            } else {
                partner.sendMessage(ChatColor.YELLOW + player.getName() + " has unaccepted the trade.");
            }
        }
    }
    
    private void updateAcceptStatus(Player player) {
        UUID partnerId = tradePartners.get(player.getUniqueId());
        if (partnerId == null) return;
        
        Player partner = Bukkit.getPlayer(partnerId);
        if (partner == null) return;
        
        Inventory inv = tradeInventories.get(player.getUniqueId());
        if (inv == null) return;
        
        // Update player status
        ItemStack yourStatus = new ItemStack(Material.WOOL, 1, 
            playerAccepted.get(player.getUniqueId()) ? (short) 5 : (short) 14);
        ItemMeta yourStatusMeta = yourStatus.getItemMeta();
        yourStatusMeta.setDisplayName(playerAccepted.get(player.getUniqueId()) ? 
            ChatColor.GREEN + "You: READY" : ChatColor.RED + "You: NOT READY");
        yourStatus.setItemMeta(yourStatusMeta);
        inv.setItem(18, yourStatus);
        
        // Update partner status
        ItemStack partnerStatus = new ItemStack(Material.WOOL, 1, 
            playerAccepted.get(partnerId) ? (short) 5 : (short) 14);
        ItemMeta partnerStatusMeta = partnerStatus.getItemMeta();
        partnerStatusMeta.setDisplayName(playerAccepted.get(partnerId) ? 
            ChatColor.GREEN + partner.getName() + ": READY" : ChatColor.RED + partner.getName() + ": NOT READY");
        partnerStatus.setItemMeta(partnerStatusMeta);
        inv.setItem(26, partnerStatus);
        
        player.updateInventory();
    }
    
    private void completeTrade(Player player1, Player player2) {
        // Get items from both players' inventories
        Inventory inv1 = tradeInventories.get(player1.getUniqueId());
        Inventory inv2 = tradeInventories.get(player2.getUniqueId());
        
        ItemStack[] p1Items = getPlayerOfferedItems(inv1);
        ItemStack[] p2Items = getPlayerOfferedItems(inv2);
        
        // Complete the trade
        plugin.getTradeManager().completeTrade(player1, player2, p1Items, p2Items);
        
        // Close inventories
        player1.closeInventory();
        player2.closeInventory();
        
        // Clean up
        cleanupTrade(player1);
        cleanupTrade(player2);
    }
    
    private ItemStack[] getPlayerOfferedItems(Inventory inv) {
        ItemStack[] items = new ItemStack[6];
        int index = 0;
        
        // Get items from player's trading area
        int[] slots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        for (int slot : slots) {
            if (slot < inv.getSize()) {
                items[index++] = inv.getItem(slot);
            }
        }
        
        return items;
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        
        if (tradeInventories.containsKey(player.getUniqueId())) {
            returnItemsToPlayer(player);
            notifyPartner(player, ChatColor.RED + player.getName() + " closed the trade window.");
            cleanupTrade(player);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        if (tradeInventories.containsKey(player.getUniqueId())) {
            returnItemsToPlayer(player);
            notifyPartner(player, ChatColor.RED + player.getName() + " disconnected during trade.");
            cleanupTrade(player);
        }
    }
    
    private void closeTradeForBothPlayers(Player player) {
        returnItemsToPlayer(player);
        notifyPartner(player, ChatColor.RED + "Trade was cancelled.");
        
        UUID partnerId = tradePartners.get(player.getUniqueId());
        if (partnerId != null) {
            Player partner = Bukkit.getPlayer(partnerId);
            if (partner != null && partner.isOnline()) {
                returnItemsToPlayer(partner);
                partner.closeInventory();
                cleanupTrade(partner);
            }
        }
        
        cleanupTrade(player);
    }
    
    private void notifyPartner(Player player, String message) {
        UUID partnerId = tradePartners.get(player.getUniqueId());
        if (partnerId != null) {
            Player partner = Bukkit.getPlayer(partnerId);
            if (partner != null && partner.isOnline()) {
                partner.sendMessage(message);
            }
        }
    }
    
    public static void returnItemsToPlayer(Player player) {
        Inventory inv = tradeInventories.get(player.getUniqueId());
        if (inv == null) return;
        
        // Get items from the player's trade area
        int[] slots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        for (int slot : slots) {
            ItemStack item = inv.getItem(slot);
            if (item != null) {
                HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
                // Drop items on ground if inventory is full
                for (ItemStack leftItem : leftOver.values()) {
                    player.getWorld().dropItem(player.getLocation(), leftItem);
                }
            }
        }
    }
    
    private void cleanupTrade(Player player) {
        UUID playerId = player.getUniqueId();
        tradeInventories.remove(playerId);
        
        UUID partnerId = tradePartners.get(playerId);
        if (partnerId != null) {
            tradePartners.remove(partnerId);
            playerAccepted.remove(partnerId);
            offeredItems.remove(partnerId);
        }
        
        tradePartners.remove(playerId);
        playerAccepted.remove(playerId);
        offeredItems.remove(playerId);
        
        plugin.getTradeManager().removeActiveTrade(player);
    }
}