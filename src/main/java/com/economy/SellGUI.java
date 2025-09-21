package com.economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellGUI implements Listener {
    
    private final EconomyPlugin plugin;
    private final Map<Player, Inventory> sellInventories;
    
    public SellGUI(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.sellInventories = new HashMap<>();
    }
    
    public void openSellMenu(Player player) {
        Inventory sellInv = Bukkit.createInventory(null, 54, ChatColor.GREEN + "💰 Sell Items to Server");
        
        // Create beautiful border
        createSellBorder(sellInv);
        
        // Add search sign in top right
        ItemStack searchItem = new ItemStack(Material.SIGN);
        ItemMeta searchMeta = searchItem.getItemMeta();
        searchMeta.setDisplayName(ChatColor.AQUA + "🔍 Search Items");
        List<String> searchLore = new ArrayList<>();
        searchLore.add(ChatColor.GRAY + "Click to search for items to sell");
        searchLore.add(ChatColor.YELLOW + "Type your search term in chat");
        searchLore.add("");
        searchLore.add(ChatColor.GREEN + "Click to search!");
        searchMeta.setLore(searchLore);
        searchItem.setItemMeta(searchMeta);
        sellInv.setItem(8, searchItem);
        
        // Add instructions with enhanced design
        ItemStack instructions = new ItemStack(Material.BOOK);
        ItemMeta instructionsMeta = instructions.getItemMeta();
        instructionsMeta.setDisplayName(ChatColor.YELLOW + "📖 How to Sell");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "1. Place items in the slots below");
        lore.add(ChatColor.GRAY + "2. Click the green 'Sell All' button");
        lore.add(ChatColor.GRAY + "3. Items will be sold at current market prices");
        lore.add(ChatColor.GRAY + "4. Money will be added to your balance");
        lore.add("");
        lore.add(ChatColor.RED + "⚠ Only Building blocks, Ores, and Food can be sold");
        instructionsMeta.setLore(lore);
        instructions.setItemMeta(instructionsMeta);
        sellInv.setItem(4, instructions);
        
        // Add sell all button with enhanced design
        ItemStack sellAllButton = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta sellAllMeta = sellAllButton.getItemMeta();
        sellAllMeta.setDisplayName(ChatColor.GREEN + "💰 Sell All Items");
        List<String> sellAllLore = new ArrayList<>();
        sellAllLore.add(ChatColor.GRAY + "Click to sell all items in the slots below");
        sellAllLore.add(ChatColor.GRAY + "Current total value will be calculated");
        sellAllLore.add("");
        sellAllLore.add(ChatColor.YELLOW + "Only tradeable items will be sold");
        sellAllLore.add(ChatColor.RED + "Non-tradeable items will be returned");
        sellAllMeta.setLore(sellAllLore);
        sellAllButton.setItemMeta(sellAllMeta);
        sellInv.setItem(49, sellAllButton);
        
        // Add close button
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "❌ Close");
        closeButton.setItemMeta(closeMeta);
        sellInv.setItem(53, closeButton);
        
        // Add current balance display
        updateBalanceDisplay(sellInv, player);
        
        sellInventories.put(player, sellInv);
        player.openInventory(sellInv);
    }
    
    private void createSellBorder(Inventory inv) {
        // Top and bottom borders with green glass
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, createGlassPane(Material.STAINED_GLASS_PANE, (short) 5, ChatColor.GREEN + "▬▬▬▬▬▬▬▬▬"));
            inv.setItem(i + 45, createGlassPane(Material.STAINED_GLASS_PANE, (short) 5, ChatColor.GREEN + "▬▬▬▬▬▬▬▬▬"));
        }
        
        // Side borders with light blue glass
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, createGlassPane(Material.STAINED_GLASS_PANE, (short) 3, ChatColor.AQUA + "▬"));
            inv.setItem(i + 8, createGlassPane(Material.STAINED_GLASS_PANE, (short) 3, ChatColor.AQUA + "▬"));
        }
        
        // Fill empty spaces with gray glass
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, createGlassPane(Material.STAINED_GLASS_PANE, (short) 7, " "));
            }
        }
    }
    
    private ItemStack createGlassPane(Material material, short data, String name) {
        ItemStack pane = new ItemStack(material, 1, data);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(new ArrayList<>());
        pane.setItemMeta(meta);
        return pane;
    }
    
    private void updateBalanceDisplay(Inventory inv, Player player) {
        double balance = plugin.getEconomyManager().getBalance(player.getUniqueId());
        ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        balanceMeta.setDisplayName(ChatColor.GOLD + "💰 Your Balance");
        
        List<String> balanceLore = new ArrayList<>();
        balanceLore.add(ChatColor.YELLOW + plugin.getEconomyManager().formatBalance(balance));
        balanceLore.add("");
        balanceLore.add(ChatColor.GRAY + "Use /balance to check anytime");
        balanceMeta.setLore(balanceLore);
        balanceItem.setItemMeta(balanceMeta);
        inv.setItem(45, balanceItem);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (!title.equals(ChatColor.GREEN + "Sell Items to Server")) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Handle special buttons
        if (clicked.getType() == Material.BARRIER) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }
        
        if (clicked.getType() == Material.EMERALD_BLOCK) {
            event.setCancelled(true);
            sellAllItems(player);
            return;
        }
        
        if (clicked.getType() == Material.SIGN) {
            event.setCancelled(true);
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Please type your search term in chat (or 'cancel' to cancel):");
            return;
        }
        
        if (clicked.getType() == Material.BOOK || clicked.getType() == Material.GOLD_INGOT) {
            event.setCancelled(true);
            return;
        }
        
        // Allow players to place items in sell slots (rows 1-5, columns 0-8)
        int slot = event.getSlot();
        if (slot >= 9 && slot <= 44) {
            // Allow normal item placement/removal
            return;
        }
        
        // Cancel clicks on other slots
        event.setCancelled(true);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        
        if (title.equals(ChatColor.GREEN + "Sell Items to Server")) {
            // Return items to player inventory
            Inventory inv = sellInventories.get(player);
            if (inv != null) {
                for (int i = 9; i <= 44; i++) {
                    ItemStack item = inv.getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        player.getInventory().addItem(item);
                    }
                }
                sellInventories.remove(player);
            }
        }
    }
    
    private void sellAllItems(Player player) {
        Inventory inv = sellInventories.get(player);
        if (inv == null) return;
        
        double totalValue = 0.0;
        int totalItems = 0;
        Map<Material, Integer> itemsToSell = new HashMap<>();
        
        // Calculate total value and collect items
        for (int i = 9; i <= 44; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                Material material = item.getType();
                int quantity = item.getAmount();
                
                if (plugin.getMarketSystem().isMaterialTradeable(material)) {
                    double pricePerItem = plugin.getMarketSystem().getCurrentPrice(material);
                    double itemValue = pricePerItem * quantity;
                    totalValue += itemValue;
                    totalItems += quantity;
                    
                    itemsToSell.put(material, itemsToSell.getOrDefault(material, 0) + quantity);
                } else {
                    // Return unsupported items to player
                    player.getInventory().addItem(item);
                    player.sendMessage(ChatColor.RED + "Cannot sell " + formatItemName(material) + " - not supported by market");
                }
                
                // Clear the slot
                inv.setItem(i, null);
            }
        }
        
        if (totalItems == 0) {
            player.sendMessage(ChatColor.YELLOW + "No items to sell!");
            return;
        }
        
        // Process the sale
        plugin.getEconomyManager().deposit(player.getUniqueId(), totalValue);
        
        // Record sales for market system
        for (Map.Entry<Material, Integer> entry : itemsToSell.entrySet()) {
            plugin.getMarketSystem().recordSale(entry.getKey(), entry.getValue());
        }
        
        // Update balance display
        updateBalanceDisplay(inv, player);
        
        // Send confirmation message
        player.sendMessage(ChatColor.GREEN + "Sold " + totalItems + " items for " + 
            plugin.getEconomyManager().formatBalance(totalValue));
        
        // Show breakdown of sold items
        if (itemsToSell.size() <= 10) { // Only show breakdown if not too many different items
            player.sendMessage(ChatColor.GRAY + "Items sold:");
            for (Map.Entry<Material, Integer> entry : itemsToSell.entrySet()) {
                double pricePerItem = plugin.getMarketSystem().getCurrentPrice(entry.getKey());
                double itemValue = pricePerItem * entry.getValue();
                player.sendMessage(ChatColor.GRAY + "  " + entry.getValue() + "x " + 
                    formatItemName(entry.getKey()) + " - " + 
                    plugin.getEconomyManager().formatBalance(itemValue));
            }
        }
    }
    
    private String formatItemName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(word.substring(0, 1).toUpperCase())
                   .append(word.substring(1));
        }
        
        return formatted.toString();
    }
}
