package com.example.vaultchest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class VaultChestPlugin extends JavaPlugin implements Listener {
    private File vaultsFolder;
    private static final String VAULT_TITLE = ChatColor.DARK_PURPLE + "Your Vault";

    @Override
    public void onEnable() {
        // Create vaults folder if it doesn't exist
        vaultsFolder = new File(getDataFolder(), "vaults");
        if (!vaultsFolder.exists()) {
            vaultsFolder.mkdirs();
        }

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Register command safely
        if (this.getCommand("vault") != null) {
            this.getCommand("vault").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                    return true;
                }
                Player player = (Player) sender;
                if (!player.hasPermission("vaultchest.use")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }
                if (args.length > 0) {
                    if (args[0].equalsIgnoreCase("reload")) {
                        if (player.hasPermission("vaultchest.reload")) {
                            reloadConfig();
                            player.sendMessage(ChatColor.GREEN + "VaultChest config reloaded.");
                        } else {
                            player.sendMessage(ChatColor.RED + "You do not have permission to reload VaultChest.");
                        }
                        return true;
                    } else if (args[0].equalsIgnoreCase("help")) {
                        player.sendMessage(ChatColor.GOLD + "VaultChest Help:");
                        player.sendMessage(ChatColor.YELLOW + "/vault - Open your personal vault");
                        player.sendMessage(ChatColor.YELLOW + "/vault reload - Reload plugin config");
                        return true;
                    }
                }
                try {
                    openVault(player);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Error opening your vault!");
                    getLogger().log(Level.SEVERE, "Error opening vault for " + player.getUniqueId(), e);
                }
                return true;
            });
        } else {
            getLogger().severe("Vault command not registered! Check plugin.yml.");
        }

        getLogger().info("VaultChestPlugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save all open vaults when the plugin disables
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory() != null &&
                player.getOpenInventory().getTitle() != null &&
                player.getOpenInventory().getTitle().equals(VAULT_TITLE)) {
                player.closeInventory();
            }
        }

        getLogger().info("VaultChestPlugin has been disabled!");
    }

    private void openVault(Player player) {
        UUID playerId = player.getUniqueId();
        File vaultFile = new File(vaultsFolder, playerId.toString() + ".yml");
        
        // Create inventory
    Inventory vault = Bukkit.createInventory(player, 27, VAULT_TITLE);
        
        // Load items if vault file exists
        if (vaultFile.exists()) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(vaultFile);
                
                for (int i = 0; i < 27; i++) {
                    if (config.contains("item" + i)) {
                        ItemStack item = config.getItemStack("item" + i);
                        vault.setItem(i, item);
                    }
                }
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "Error loading your vault!");
                getLogger().log(Level.SEVERE, "Failed to load vault for " + playerId, e);
            }
        }
        
        player.openInventory(vault);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            Inventory inv = event.getInventory();
            String title = event.getView().getTitle();
            if (title != null && title.equals(VAULT_TITLE)) {
                // Save vault contents to file
                try {
                    saveVault(player.getUniqueId(), inv.getContents());
                    player.sendMessage(ChatColor.GREEN + "Vault saved!");
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Error saving your vault!");
                    getLogger().log(Level.SEVERE, "Error saving vault for " + player.getUniqueId(), e);
                }
            }
        }
    }
    
    private void saveVault(UUID playerId, ItemStack[] contents) {
        File vaultFile = new File(vaultsFolder, playerId.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        try {
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null && contents[i].getType() != Material.AIR) {
                    config.set("item" + i, contents[i]);
                }
            }
            
            config.save(vaultFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to save vault for " + playerId, e);
        }
    }
}