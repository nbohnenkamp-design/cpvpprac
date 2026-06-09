# CPVPSingleBiome v1.2.11

A PvP-focused world generation and arena management plugin for Paper servers.

CPVPSingleBiome creates clean, competitive worlds designed for Crystal PvP, Practice PvP, KitPvP, FFA arenas and events. Unlike traditional survival generators, it focuses on fair combat terrain, automated world resets, fast world deployment and long-term server maintenance.

Originally developed and actively used on **cpvpprac.eu**, a Crystal PvP practice server.

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

### Tested

| Plugin Version | Server Software | Status |
| -------------- | --------------- | ------ |
| 1.2.11         | Paper 1.21.11   | Tested |
| 1.2.11         | Paper 26.1.2    | Tested |

### Requirements

* Java 21+
* Paper Server

### Optional Integrations

| Plugin          | Purpose                       |
| --------------- | ----------------------------- |
| Multiverse-Core | World creation and management |
| Chunky          | Pregeneration support         |

Other Paper versions may work but are not officially tested.

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

Reset worlds automatically restore:

* HARD difficulty
* SURVIVAL gamemode
* PvP enabled
* configured WorldBorder

---

### Chunky Integration

Direct integration with Chunky.

* Automatic pregeneration support
* Multi-world support
* Reduced lag spikes after creation
* Faster deployment of new worlds

---

### World Management

Manage biome worlds directly from in-game commands.

Examples:

* Create biome worlds
* Teleport between worlds
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

The plugin was originally developed for **cpvpprac.eu** and has been tested in real PvP environments before being released as open source.

---

## Production Usage

CPVPSingleBiome is actively used on:

```text
cpvpprac.eu
```

A Crystal PvP practice server focused on open combat arenas and competitive gameplay.

---

## Open Source

CPVPSingleBiome is released under the GPL-3.0-only license.

Source code is available on GitHub.

---

## Disclaimer

This project is not affiliated with Mojang, Microsoft, Minecraft or PaperMC.

Minecraft is a trademark of Microsoft.


