# CPVPSingleBiome

A PvP-focused world generation and arena management plugin for Paper and Purpur servers.

CPVPSingleBiome creates clean, competitive worlds designed for Crystal PvP, Practice PvP, KitPvP, FFA arenas and events. Unlike traditional survival generators, it focuses on fair combat terrain, automated world resets, fast world deployment and long-term server maintenance.

> **Recommended plugins:** [Multiverse-Core](https://modrinth.com/plugin/multiverse-core) for multi-world management and [Chunky](https://modrinth.com/plugin/chunky) for world pregeneration. Both are optional. CPVPSingleBiome also works standalone.

---

## Designed For

* Crystal PvP
* Practice PvP
* KitPvP
* FFA Arenas
* Event Worlds
* Tournament Servers

---

## Compatibility

### Requirements

* Paper or Purpur server
* Java runtime compatible with the chosen server version

Check the individual plugin releases for tested and supported Minecraft versions.

### Optional Integrations

| Plugin | Purpose |
| --- | --- |
| Multiverse-Core | World creation and management |
| Chunky | Pregeneration support |

CPVPSingleBiome supports standalone world creation, automatic world loading and world resets without Multiverse-Core.

---

## Available Biomes

* Plains
* Desert
* Badlands
* Snow
* Mushroom
* End

Each biome is designed around PvP readability while maintaining a unique atmosphere.

---

## Features

### PvP-Focused Terrain

Designed specifically for competitive gameplay.

* Open terrain
* Clean sightlines
* Reduced clutter
* Gentle elevation changes
* Fair combat environments
* Consistent terrain generation

Perfect for:

* Crystal PvP
* Practice PvP
* Duels
* FFA Worlds
* Event Arenas

---

### Automated Arena Resets

Keep combat worlds fresh automatically.

Features include:

* Scheduled resets
* Manual resets
* Automatic world recreation
* Automatic world configuration
* WorldBorder restoration
* Player protection during resets
* Compatibility with modern Paper world storage
* Optional world backups

Reset worlds automatically restore the configured:

* Difficulty
* Gamemode
* PvP settings
* WorldBorder

### World Backup Management

CPVPSingleBiome supports server-side world backups during resets.

* Optional backup creation before world resets
* Backup support for different Paper world storage layouts
* Backup preservation if world recreation fails
* Configurable backup retention
* Option to keep only the latest backup per world
* Older backups are removed only after a successful reset when cleanup is enabled

Backups are stored on the Minecraft server under:

`plugins/CPVPSingleBiome/server-world-backups/`

---

### Chunky Integration

Direct integration with Chunky.

* Automatic pregeneration support
* Multi-world support
* Reduced lag spikes caused by chunk generation
* Faster deployment of pregenerated worlds

---

### World Management

Manage biome worlds directly from in-game commands.

Features include:

* Create biome worlds
* Teleport between worlds
* Automatically load plugin-managed worlds after server restarts
* Store world information in a dedicated `worlds.yml`
* Standalone operation without Multiverse-Core
* Automatic integration with Multiverse-Core when installed
* View world information
* Start Chunky pregeneration
* Reset worlds
* Monitor reset status

---

### Highly Configurable

Customize nearly every aspect of world generation.

Available settings include:

* Terrain height
* Hill intensity
* Flatness
* Noise scale
* Decoration density
* World border size
* Reset schedules
* World backup settings
* Chunky settings
* Biome-specific overrides

Server owners can create anything from almost completely flat PvP maps to more natural-looking practice worlds.

---

## Commands

### General

```text
/cpvpsb help
/cpvpsb reload
/cpvpsb biomes
/cpvpsb info
```

### World Management

```text
/cpvpsb create <world> [biome]
/cpvpsb tp <world>
```

### Reset System

```text
/cpvpsb reset now
/cpvpsb reset <world>
/cpvpsb reset status
/cpvpsb reset reload
```

### Chunky Integration

```text
/cpvpsb chunky start <world>
/cpvpsb chunky start-all
```

---

## Snow Biome Features

The Snow biome includes additional atmosphere features:

* Light snowflake particles
* Thin snow layers
* Flat packed-ice ponds
* Sparse rock formations
* Rare spruce trees
* PvP-friendly visibility

---

## Why This Plugin Exists

Most world generators focus on exploration and survival gameplay.

CPVPSingleBiome was created for Crystal PvP communities that require worlds which are visually interesting while remaining fair, performant and enjoyable for competitive combat.

---

## Download & Source Code

CPVPSingleBiome is available on:

- **[Modrinth](https://modrinth.com/plugin/cpvpsinglebiome)**
- **[Hangar (PaperMC)](https://hangar.papermc.io/nbohnenkamp-design/CPVPSingleBiome)**
- **[GitHub Releases](https://github.com/nbohnenkamp-design/cpvpprac/releases)**

CPVPSingleBiome is open source and licensed under **GPL-3.0-only**.

Developed by **Norbert Bohnenkamp**.

Source code is available on [GitHub](https://github.com/nbohnenkamp-design/cpvpprac).

---

## Disclaimer

This project is not affiliated with Mojang, Microsoft, Minecraft or PaperMC.

Minecraft is a trademark of Microsoft.
