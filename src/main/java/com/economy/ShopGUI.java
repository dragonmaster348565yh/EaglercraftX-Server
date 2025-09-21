package com.economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopGUI implements Listener {
    
    private final EconomyPlugin plugin;
    private final Map<String, List<Material>> categories;
    private final Map<Player, ShopSession> playerSessions;
    private final int ITEMS_PER_PAGE = 28; // 4 rows of 7 items
    
    public ShopGUI(EconomyPlugin plugin) {
        this.plugin = plugin;
        this.categories = new HashMap<>();
        this.playerSessions = new HashMap<>();
        loadCategories();
    }
    
    private void loadCategories() {
        ConfigurationSection categorySection = plugin.getConfig().getConfigurationSection("shop-categories");
        if (categorySection != null) {
            for (String categoryName : categorySection.getKeys(false)) {
                ConfigurationSection category = categorySection.getConfigurationSection(categoryName);
                if (category != null) {
                    List<Material> items = new ArrayList<>();
                    List<String> itemNames = category.getStringList("items");
                    
                    for (String itemName : itemNames) {
                        try {
                            Material material = Material.valueOf(itemName);
                            items.add(material);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid material in shop category " + categoryName + ": " + itemName);
                        }
                    }
                    
                    categories.put(categoryName, items);
                }
            }
        }
    }
    
    public void openShop(Player player) {
        ShopSession session = new ShopSession();
        session.currentCategory = "blocks";
        session.currentPage = 0;
        session.searchTerm = "";
        session.player = player;
        session.cartItem = null;
        session.cartQuantity = 0;
        session.isReady = false;
        playerSessions.put(player, session);
        
        openCategory(player, "blocks");
    }
    
    public void openCategory(Player player, String categoryName) {
        ShopSession session = playerSessions.get(player);
        if (session == null) {
            session = new ShopSession();
            session.player = player;
            playerSessions.put(player, session);
        }
        
        session.currentCategory = categoryName;
        session.currentPage = 0;
        
        List<Material> items = categories.get(categoryName);
        if (items == null) {
            player.sendMessage(ChatColor.RED + "Category not found!");
            return;
        }
        
        // Filter items based on search term
        List<Material> filteredItems = filterItems(items, session.searchTerm);
        
        int totalPages = (int) Math.ceil((double) filteredItems.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        
        session.totalPages = totalPages;
        session.filteredItems = filteredItems;
        
        Inventory categoryInv = Bukkit.createInventory(null, 54, 
            ChatColor.GOLD + "Shop - " + categoryName + " (Page " + (session.currentPage + 1) + "/" + totalPages + ")");
        
        // Add items for current page
        int startIndex = session.currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredItems.size());
        
        int slot = 10; // Start from row 2, column 1
        for (int i = startIndex; i < endIndex; i++) {
            Material material = filteredItems.get(i);
            double price = plugin.getMarketSystem().getCurrentPrice(material);
            ItemStack item = createShopItem(material, price);
            categoryInv.setItem(slot, item);
            
            slot++;
            if (slot % 9 == 8) slot += 2; // Skip to next row, avoid rightmost column
        }
        
        // Add navigation buttons
        addNavigationButtons(categoryInv, session);
        
        player.openInventory(categoryInv);
    }
    
    private List<Material> filterItems(List<Material> items, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>(items);
        }
        
        List<Material> filtered = new ArrayList<>();
        String search = searchTerm.toLowerCase();
        
        for (Material material : items) {
            String itemName = material.name().toLowerCase();
            String displayName = formatItemName(material).toLowerCase();
            
            if (itemName.contains(search) || displayName.contains(search)) {
                filtered.add(material);
            }
        }
        
        return filtered;
    }
    
    private ItemStack createShopItem(Material material, double price) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        
        String displayName = formatItemName(material);
        meta.setDisplayName(ChatColor.WHITE + "✦ " + displayName);
        
        String priceChange = plugin.getMarketSystem().getPriceChangeIndicator(material);
        String supplyDemand = plugin.getMarketSystem().getSupplyDemandStatus(material);
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GOLD + "💰 Price: " + ChatColor.YELLOW + plugin.getEconomyManager().formatBalance(price) + " " + priceChange);
        lore.add(ChatColor.GRAY + "📊 " + supplyDemand);
        lore.add("");
        lore.add(ChatColor.GREEN + "🖱️ Click to purchase");
        lore.add(ChatColor.YELLOW + "⚡ Shift-click for quick buy (1 item)");
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private void addNavigationButtons(Inventory inv, ShopSession session) {
        // Search sign in top right
        ItemStack searchItem = new ItemStack(Material.SIGN);
        ItemMeta searchMeta = searchItem.getItemMeta();
        searchMeta.setDisplayName(ChatColor.AQUA + "🔍 Search Items");
        List<String> searchLore = new ArrayList<>();
        searchLore.add(ChatColor.GRAY + "Current search: " + (session.searchTerm.isEmpty() ? "None" : session.searchTerm));
        searchLore.add("");
        searchLore.add(ChatColor.YELLOW + "Click to search for items");
        searchLore.add(ChatColor.GREEN + "Type your search term in chat");
        searchMeta.setLore(searchLore);
        searchItem.setItemMeta(searchMeta);
        inv.setItem(8, searchItem);
        
        // Category buttons with enhanced design
        String[] categoryOrder = {"blocks", "ores", "food"};
        String[] categoryIcons = {"BRICK", "DIAMOND", "BREAD"};
        String[] categoryEmojis = {"🧱", "💎", "🍞"};
        
        for (int i = 0; i < categoryOrder.length; i++) {
            String categoryName = categoryOrder[i];
            String icon = categoryIcons[i];
            String emoji = categoryEmojis[i];
            
            ConfigurationSection category = plugin.getConfig().getConfigurationSection("shop-categories." + categoryName);
            if (category != null) {
                String displayName = ChatColor.translateAlternateColorCodes('&', category.getString("name", categoryName));
                displayName = emoji + " " + displayName;
                if (categoryName.equals(session.currentCategory)) {
                    displayName = ChatColor.BOLD + displayName + ChatColor.GREEN + " ✓";
                }
                
                ItemStack categoryItem = new ItemStack(Material.valueOf(icon));
                ItemMeta meta = categoryItem.getItemMeta();
                meta.setDisplayName(displayName);
                
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Click to browse " + categoryName + " items");
                lore.add("");
                lore.add(ChatColor.YELLOW + "Items: " + ChatColor.WHITE + categories.get(categoryName).size());
                if (categoryName.equals(session.currentCategory)) {
                    lore.add(ChatColor.GREEN + "Currently viewing");
                }
                meta.setLore(lore);
                categoryItem.setItemMeta(meta);
                
                inv.setItem(1 + i, categoryItem);
            }
        }
        
        // Page navigation
        if (session.totalPages > 1) {
            // Previous page
            if (session.currentPage > 0) {
                ItemStack prevItem = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevItem.getItemMeta();
                prevMeta.setDisplayName(ChatColor.GREEN + "◀ Previous Page");
                prevMeta.setLore(List.of(ChatColor.GRAY + "Go to page " + session.currentPage));
                prevItem.setItemMeta(prevMeta);
                inv.setItem(45, prevItem);
            }
            
            // Next page
            if (session.currentPage < session.totalPages - 1) {
                ItemStack nextItem = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextItem.getItemMeta();
                nextMeta.setDisplayName(ChatColor.GREEN + "Next Page ▶");
                nextMeta.setLore(List.of(ChatColor.GRAY + "Go to page " + (session.currentPage + 2)));
                nextItem.setItemMeta(nextMeta);
                inv.setItem(53, nextItem);
            }
            
            // Page info
            ItemStack pageInfo = new ItemStack(Material.PAPER);
            ItemMeta pageMeta = pageInfo.getItemMeta();
            pageMeta.setDisplayName(ChatColor.YELLOW + "📄 Page " + (session.currentPage + 1) + " of " + session.totalPages);
            pageMeta.setLore(List.of(ChatColor.GRAY + "Use arrows to navigate"));
            pageInfo.setItemMeta(pageMeta);
            inv.setItem(49, pageInfo);
        } else {
            // Close button if no pagination
            ItemStack closeItem = new ItemStack(Material.BARRIER);
            ItemMeta closeMeta = closeItem.getItemMeta();
            closeMeta.setDisplayName(ChatColor.RED + "❌ Close Shop");
            closeItem.setItemMeta(closeMeta);
            inv.setItem(49, closeItem);
        }
        
        // Balance display
        updateBalanceDisplay(inv, session.player);
    }
    
    private void updateBalanceDisplay(Inventory inv, Player player) {
        if (player == null) return;
        
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
        inv.setItem(48, balanceItem);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        ShopSession session = playerSessions.get(player);
        if (session == null) return;
        
        // Handle checkout UI
        if (title.startsWith(ChatColor.GOLD + "Checkout - ")) {
            handleCheckoutClick(player, clicked, session);
            return;
        }
        
        // Handle main shop UI
        if (!title.startsWith(ChatColor.GOLD + "Shop - ")) return;
        
        // Handle special buttons
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            playerSessions.remove(player);
            return;
        }
        
        if (clicked.getType() == Material.SIGN) {
            // Search functionality
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Please type your search term in chat (or 'cancel' to cancel):");
            // Note: In a real implementation, you'd need to handle chat input
            return;
        }
        
        if (clicked.getType() == Material.ARROW) {
            // Page navigation
            if (clicked.getItemMeta().getDisplayName().contains("Previous")) {
                session.currentPage = Math.max(0, session.currentPage - 1);
            } else if (clicked.getItemMeta().getDisplayName().contains("Next")) {
                session.currentPage = Math.min(session.totalPages - 1, session.currentPage + 1);
            }
            openCategory(player, session.currentCategory);
            return;
        }
        
        if (clicked.getType() == Material.CHEST) {
            // Category selection
            String categoryName = null;
            for (String cat : categories.keySet()) {
                if (clicked.getItemMeta().getDisplayName().contains(cat)) {
                    categoryName = cat;
                    break;
                }
            }
            
            if (categoryName != null) {
                openCategory(player, categoryName);
            }
            return;
        }
        
        // Handle item selection - open checkout UI
        Material material = clicked.getType();
        if (!plugin.getMarketSystem().isMaterialTradeable(material)) {
            player.sendMessage(ChatColor.RED + "This item is not available for purchase!");
            return;
        }
        
        // Open checkout UI
        openCheckoutUI(player, material);
    }
    
    private void handleCheckoutClick(Player player, ItemStack clicked, ShopSession session) {
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            playerSessions.remove(player);
            return;
        }
        
        if (clicked.getType() == Material.ARROW) {
            // Back to shop
            player.closeInventory();
            openCategory(player, session.currentCategory);
            return;
        }
        
        if (clicked.getType() == Material.WOOL) {
            // Handle quantity buttons
            String displayName = clicked.getItemMeta().getDisplayName();
            int change = 0;
            
            if (displayName.contains("Add 1")) change = 1;
            else if (displayName.contains("Add 10")) change = 10;
            else if (displayName.contains("Add 64")) change = 64;
            else if (displayName.contains("Remove 1")) change = -1;
            else if (displayName.contains("Remove 10")) change = -10;
            else if (displayName.contains("Remove 64")) change = -64;
            
            if (change != 0) {
                session.cartQuantity = Math.max(0, session.cartQuantity + change);
                updateCheckoutUI(player, session);
            }
            return;
        }
        
        if (clicked.getType() == Material.STAINED_GLASS_PANE) {
            // Handle confirm button
            if (session.cartQuantity > 0) {
                if (!session.isReady) {
                    // First click - turn blue and ask for confirmation
                    session.isReady = true;
                    updateCheckoutUI(player, session);
                    player.sendMessage(ChatColor.YELLOW + "Click again to confirm purchase!");
                } else {
                    // Second click - complete purchase
                    double price = plugin.getMarketSystem().getCurrentPrice(session.cartItem);
                    processPurchase(player, session.cartItem, session.cartQuantity, price);
                }
            }
            return;
        }
    }
    
    private void updateCheckoutUI(Player player, ShopSession session) {
        Inventory checkoutInv = player.getOpenInventory().getTopInventory();
        
        // Update item display
        double price = plugin.getMarketSystem().getCurrentPrice(session.cartItem);
        ItemStack displayItem = new ItemStack(session.cartItem, 1);
        ItemMeta displayMeta = displayItem.getItemMeta();
        displayMeta.setDisplayName(ChatColor.WHITE + formatItemName(session.cartItem));
        List<String> displayLore = new ArrayList<>();
        displayLore.add(ChatColor.GREEN + "Price: " + plugin.getEconomyManager().formatBalance(price));
        displayLore.add(ChatColor.YELLOW + "Quantity: " + session.cartQuantity);
        displayLore.add(ChatColor.GOLD + "Total: " + plugin.getEconomyManager().formatBalance(price * session.cartQuantity));
        displayMeta.setLore(displayLore);
        displayItem.setItemMeta(displayMeta);
        checkoutInv.setItem(35, displayItem);
        
        // Update confirm button
        ItemStack confirmItem;
        if (session.cartQuantity == 0) {
            // Red - not ready
            confirmItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14);
            ItemMeta confirmMeta = confirmItem.getItemMeta();
            confirmMeta.setDisplayName(ChatColor.RED + "Not Ready");
            List<String> confirmLore = new ArrayList<>();
            confirmLore.add(ChatColor.GRAY + "Add items to cart first");
            confirmMeta.setLore(confirmLore);
            confirmItem.setItemMeta(confirmMeta);
            session.isReady = false;
        } else if (!session.isReady) {
            // Blue - ready for confirmation
            confirmItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 11);
            ItemMeta confirmMeta = confirmItem.getItemMeta();
            confirmMeta.setDisplayName(ChatColor.BLUE + "Click to Confirm");
            List<String> confirmLore = new ArrayList<>();
            confirmLore.add(ChatColor.GRAY + "Click to confirm purchase");
            confirmMeta.setLore(confirmLore);
            confirmItem.setItemMeta(confirmMeta);
        } else {
            // Green - confirmed
            confirmItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 13);
            ItemMeta confirmMeta = confirmItem.getItemMeta();
            confirmMeta.setDisplayName(ChatColor.GREEN + "Confirm Purchase");
            List<String> confirmLore = new ArrayList<>();
            confirmLore.add(ChatColor.GRAY + "Click to complete purchase");
            confirmMeta.setLore(confirmLore);
            confirmItem.setItemMeta(confirmMeta);
        }
        checkoutInv.setItem(40, confirmItem);
    }
    
    private void openCheckoutUI(Player player, Material material) {
        ShopSession session = playerSessions.get(player);
        if (session == null) return;
        
        session.cartItem = material;
        session.cartQuantity = 0;
        session.isReady = false;
        
        Inventory checkoutInv = Bukkit.createInventory(null, 54, 
            ChatColor.GOLD + "🛒 Checkout - " + formatItemName(material));
        
        // Create beautiful border
        createCheckoutBorder(checkoutInv);
        
        // Item display on the right with enhanced design
        double price = plugin.getMarketSystem().getCurrentPrice(material);
        String priceChange = plugin.getMarketSystem().getPriceChangeIndicator(material);
        ItemStack displayItem = new ItemStack(material, 1);
        ItemMeta displayMeta = displayItem.getItemMeta();
        displayMeta.setDisplayName(ChatColor.WHITE + "✦ " + formatItemName(material));
        
        List<String> displayLore = new ArrayList<>();
        displayLore.add("");
        displayLore.add(ChatColor.GOLD + "💰 Price: " + ChatColor.YELLOW + plugin.getEconomyManager().formatBalance(price) + " " + priceChange);
        displayLore.add(ChatColor.GRAY + "📊 " + plugin.getMarketSystem().getSupplyDemandStatus(material));
        displayLore.add("");
        displayLore.add(ChatColor.AQUA + "📦 Quantity: " + ChatColor.WHITE + session.cartQuantity);
        displayLore.add(ChatColor.GREEN + "💵 Total: " + ChatColor.WHITE + plugin.getEconomyManager().formatBalance(price * session.cartQuantity));
        displayLore.add("");
        displayLore.add(ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━━━━━━━");
        displayMeta.setLore(displayLore);
        displayItem.setItemMeta(displayMeta);
        checkoutInv.setItem(35, displayItem);
        
        // Enhanced quantity control buttons
        createQuantityButtons(checkoutInv);
        
        // Enhanced confirm button
        createConfirmButton(checkoutInv, session);
        
        // Navigation buttons
        createNavigationButtons(checkoutInv);
        
        player.openInventory(checkoutInv);
    }
    
    private void createCheckoutBorder(Inventory inv) {
        // Top and bottom borders with gold glass
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, createGlassPane(Material.STAINED_GLASS_PANE, (short) 1, ChatColor.GOLD + "▬▬▬▬▬▬▬▬▬"));
            inv.setItem(i + 45, createGlassPane(Material.STAINED_GLASS_PANE, (short) 1, ChatColor.GOLD + "▬▬▬▬▬▬▬▬▬"));
        }
        
        // Side borders with light blue glass
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, createGlassPane(Material.STAINED_GLASS_PANE, (short) 3, ChatColor.AQUA + "▬"));
            inv.setItem(i + 8, createGlassPane(Material.STAINED_GLASS_PANE, (short) 3, ChatColor.AQUA + "▬"));
        }
        
        // Middle barrier with red glass
        for (int i = 0; i < 54; i += 9) {
            inv.setItem(i + 4, createGlassPane(Material.STAINED_GLASS_PANE, (short) 14, ChatColor.RED + "▬"));
        }
        
        // Fill empty spaces with gray glass
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, createGlassPane(Material.STAINED_GLASS_PANE, (short) 7, " "));
            }
        }
    }
    
    private void createQuantityButtons(Inventory inv) {
        // Green wool buttons (add) with enhanced design
        String[] addButtons = {"Add 1", "Add 10", "Add 64"};
        int[] addSlots = {20, 21, 22};
        String[] addEmojis = {"➕", "🔟", "📦"};
        
        for (int i = 0; i < addButtons.length; i++) {
            ItemStack button = new ItemStack(Material.WOOL, 1, (short) 13); // Green
            ItemMeta meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + addEmojis[i] + " " + addButtons[i]);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to " + addButtons[i].toLowerCase());
            lore.add(ChatColor.YELLOW + "Left-click to add");
            meta.setLore(lore);
            button.setItemMeta(meta);
            inv.setItem(addSlots[i], button);
        }
        
        // Red wool buttons (remove) with enhanced design
        String[] removeButtons = {"Remove 1", "Remove 10", "Remove 64"};
        int[] removeSlots = {29, 30, 31};
        String[] removeEmojis = {"➖", "🔟", "📦"};
        
        for (int i = 0; i < removeButtons.length; i++) {
            ItemStack button = new ItemStack(Material.WOOL, 1, (short) 14); // Red
            ItemMeta meta = button.getItemMeta();
            meta.setDisplayName(ChatColor.RED + removeEmojis[i] + " " + removeButtons[i]);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click to " + removeButtons[i].toLowerCase());
            lore.add(ChatColor.YELLOW + "Left-click to remove");
            meta.setLore(lore);
            button.setItemMeta(meta);
            inv.setItem(removeSlots[i], button);
        }
    }
    
    private void createConfirmButton(Inventory inv, ShopSession session) {
        // Confirm button (starts red)
        ItemStack confirmItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 14); // Red
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.RED + "❌ Not Ready");
        
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.GRAY + "Add items to cart first");
        confirmLore.add("");
        confirmLore.add(ChatColor.YELLOW + "Click to add items to cart");
        confirmMeta.setLore(confirmLore);
        confirmItem.setItemMeta(confirmMeta);
        inv.setItem(40, confirmItem);
    }
    
    private void createNavigationButtons(Inventory inv) {
        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.GREEN + "◀ Back to Shop");
        backMeta.setLore(List.of(ChatColor.GRAY + "Return to shop"));
        backItem.setItemMeta(backMeta);
        inv.setItem(45, backItem);
        
        // Close button
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "❌ Close");
        closeItem.setItemMeta(closeMeta);
        inv.setItem(53, closeItem);
    }
    
    private ItemStack createGlassPane(Material material, short data, String name) {
        ItemStack pane = new ItemStack(material, 1, data);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(new ArrayList<>());
        pane.setItemMeta(meta);
        return pane;
    }
    
    public void processPurchase(Player player, Material material, int quantity, double pricePerItem) {
        double totalPrice = pricePerItem * quantity;
        
        // Validate quantity
        if (quantity <= 0 || quantity > 10000) {
            player.sendMessage(ChatColor.RED + "Invalid quantity! Must be between 1 and 10,000.");
            return;
        }
        
        // Check if player has enough money
        if (!plugin.getEconomyManager().hasEnough(player.getUniqueId(), totalPrice)) {
            player.sendMessage(ChatColor.RED + "Insufficient funds! You need " + 
                plugin.getEconomyManager().formatBalance(totalPrice));
            return;
        }
        
        // Check inventory space
        int requiredSlots = (int) Math.ceil((double) quantity / material.getMaxStackSize());
        if (player.getInventory().firstEmpty() == -1 && 
            countEmptySlots(player) < requiredSlots) {
            player.sendMessage(ChatColor.RED + "Not enough inventory space!");
            return;
        }
        
        // Process purchase
        plugin.getEconomyManager().withdraw(player.getUniqueId(), totalPrice);
        
        // Give items in stacks
        int remaining = quantity;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, material.getMaxStackSize());
            player.getInventory().addItem(new ItemStack(material, stackSize));
            remaining -= stackSize;
        }
        
        // Record purchase for market system
        plugin.getMarketSystem().recordPurchase(material, quantity);
        
        player.sendMessage(ChatColor.GREEN + "Purchased " + quantity + "x " + 
            formatItemName(material) + " for " + 
            plugin.getEconomyManager().formatBalance(totalPrice));
        
        // Close checkout and return to shop
        player.closeInventory();
        openCategory(player, playerSessions.get(player).currentCategory);
    }
    
    private int countEmptySlots(Player player) {
        int emptySlots = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                emptySlots++;
            }
        }
        return emptySlots;
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
    
    // Inner class to track player shop sessions
    private static class ShopSession {
        String currentCategory;
        int currentPage;
        int totalPages;
        String searchTerm;
        List<Material> filteredItems;
        Player player;
        Material cartItem;
        int cartQuantity;
        boolean isReady;
    }
}