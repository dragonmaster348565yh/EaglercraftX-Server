# EconomyPlugin for EaglercraftX 1.8.8

A comprehensive economy plugin with dynamic market system and supply/demand mechanics for EaglercraftX 1.8.8 servers.

## 🎮 Features

### Commands
- `/balance` or `/bal` - Check your current balance
- `/shop` - Open shop to buy items from server
- `/sell` or `/s` - Open sell menu to sell items to server

### Dynamic Market System
- **Supply & Demand Mechanics** - Prices fluctuate based on what players buy/sell
- **Realistic Pricing** - Diamonds are $100, dirt is $0.02, etc.
- **Anti-Manipulation** - Prices can't be artificially inflated/deflated
- **Restricted Trading** - Only Building blocks, Ores, and Food items can be traded
- **Market Indicators** - Shows if prices are rising/falling and supply/demand status

### Shop System
- **Categorized Shop** - Building blocks, ores, and food items only
- **Pagination** - Browse through multiple pages of items
- **Search Functionality** - Click sign in top-right to search for items
- **Trading-Style Checkout** - Click any item to open checkout UI
- **Visual Quantity Selection** - Green/Red wool buttons for adding/removing items
- **Color-Coded Confirmation** - Red (not ready) → Blue (confirm) → Green (purchase)
- **Real-time Prices** - Shows current market prices with indicators
- **Intuitive UI** - Easy to use interface similar to trading system

### Sell System
- **Easy Selling** - Place items in slots and click "Sell All"
- **Search Functionality** - Click sign in top-right to search for items to sell
- **Market Integration** - Your sales affect supply/demand
- **Instant Payment** - Money added to balance immediately
- **Visual Interface** - Clean, organized sell interface

## 🚀 Installation

### Prerequisites
- EaglercraftX 1.8.8 Server
- Spigot API 1.8.8 JAR file

### Compilation
1. **Get Spigot API:**
   - Download BuildTools.jar from [SpigotMC](https://www.spigotmc.org/wiki/buildtools/)
   - Run: `java -jar BuildTools.jar --rev 1.8.8`
   - Copy `spigot-api-1.8.8-R0.1-SNAPSHOT.jar` to `misc/spigot-api-1.8.8.jar`

2. **Compile Plugin:**
   ```bash
   # Windows
   compile.bat
   
   # Or manually
   javac -cp "../misc/spigot-api-1.8.8.jar" -d target/classes src/main/java/com/economy/*.java
   jar cf target/EconomyPlugin-1.0.0.jar -C target/classes .
   ```

3. **Install:**
   - Copy `target/EconomyPlugin-1.0.0.jar` to your server's `plugins` folder
   - Restart your server

## ⚙️ Configuration

Edit `src/main/resources/config.yml` to customize:

### Currency Settings
```yaml
currency:
  symbol: "$"
  format: "%,.2f"
  starting-balance: 100.0
```

### Market Settings
```yaml
market:
  update-interval: 5  # minutes
  fluctuation-range: 0.15  # 15% max change
  supply-demand-impact: 0.3
```

### Item Prices
All item prices are configurable in the `item-prices` section. Examples:
- Diamond: $100.00
- Iron Ingot: $2.00
- Dirt: $0.02
- And 100+ more items!

## 🎯 How It Works

1. **Players start with $100** (configurable)
2. **Market updates every 5 minutes** with realistic price fluctuations
3. **Supply/Demand affects prices** - if many players sell diamonds, price drops
4. **Shop categories** make it easy to find items
5. **Sell interface** lets players easily sell their items
6. **Admin commands** for server management

## 📁 Project Structure

```
EconomyPlugin/
├── src/main/java/com/economy/
│   ├── EconomyPlugin.java      # Main plugin class
│   ├── EconomyManager.java     # Handles player balances
│   ├── MarketSystem.java       # Dynamic pricing system
│   ├── ShopGUI.java           # Shop interface
│   └── SellGUI.java           # Sell interface
├── src/main/resources/
│   ├── plugin.yml             # Plugin configuration
│   └── config.yml             # Item prices and settings
├── pom.xml                    # Maven build file
├── compile.bat               # Windows compilation script
└── README.md                 # This file
```

## 🔧 Development

### Building with Maven
```bash
mvn clean package
```

### Manual Compilation
```bash
# Compile
javac -cp "path/to/spigot-api-1.8.8.jar" -d target/classes src/main/java/com/economy/*.java

# Create JAR
jar cf target/EconomyPlugin-1.0.0.jar -C target/classes .
```

## 📝 License

This project is open source and available under the MIT License.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 🐛 Issues

If you encounter any issues, please create an issue on GitHub with:
- Server version
- Plugin version
- Error messages
- Steps to reproduce

---

**Made for EaglercraftX 1.8.8 servers with ❤️**
