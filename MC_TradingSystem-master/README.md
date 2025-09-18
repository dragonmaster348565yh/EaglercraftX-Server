# MC_TradingSystem-master

A polished trading plugin for Spigot, Paper, and EaglercraftX 1.8.8 servers.

## Features
- Secure item and XP trading between players
- English-only language support
- No permissions required; all players can use commands
- Designed for compatibility with most 1.8.8 servers

## Installation
1. Build the plugin using Maven (`mvn package`).
2. Place the generated JAR file in your server's `plugins` folder.
3. Restart your server.

## Usage
- `/trade [player]` — Initiate a trade with another player
- `/trade accept [player]` — Accept a trade request
- `/trade decline [player]` — Decline a trade request
- `/trade cancel [player]` — Cancel a trade request
- `/trade language ...` — Manage language files (admin features, now open to all)

## Compatibility
- Spigot 1.8.8
- Paper 1.8.8
- EaglercraftX 1.8.8
- For BungeeCord support, use a messaging channel or a dedicated Bungee plugin

## Configuration
- See `config.yml` for options
- Only `en-US.yml` is used for language

## Support
- Issues: Open on GitHub
- Author: JustOneDeveloper

---
This plugin is open source and can be modified for your server needs.
