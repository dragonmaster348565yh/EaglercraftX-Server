# Economy Plugin Installation Guide

## 🎯 What You Need

To compile this economy plugin, you need the **Spigot 1.8.8 API JAR file**.

## 📥 How to Get the API

### Method 1: BuildTools (Recommended)
1. Download BuildTools.jar from: https://www.spigotmc.org/wiki/buildtools/
2. Place it in your server folder
3. Run: `java -jar BuildTools.jar --rev 1.8.8`
4. Find the generated file: `Spigot/Spigot-API/target/spigot-api-1.8.8-R0.1-SNAPSHOT.jar`
5. Copy it to `misc/spigot-api-1.8.8.jar`

### Method 2: Manual Download
Try these direct links:
- https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/1.8.8-R0.1-SNAPSHOT/
- https://repo.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/1.8.8-R0.1-SNAPSHOT/

## 🔧 Compilation Steps

1. **Get the API JAR** (using Method 1 or 2 above)
2. **Place it in** `misc/spigot-api-1.8.8.jar`
3. **Run:** `compile.bat`
4. **Copy the generated JAR** from `target/EconomyPlugin-1.0.0.jar` to your server's `plugins` folder

## 🎮 Plugin Features

- `/s` or `/sell` - Sell items to server
- `/balance` - Check your money
- `/shop` - Buy items from server
- `/pay <player> <amount>` - Send money
- Dynamic market with supply/demand
- Realistic pricing (diamonds $100, dirt $0.02)

## ⚙️ Configuration

Edit `src/main/resources/config.yml` to customize:
- Item prices
- Market settings
- Currency symbol
- Starting balance

The plugin is ready to compile once you have the Spigot API!
