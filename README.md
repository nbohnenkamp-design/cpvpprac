# CPVPSingleBiome v1.2.10 Stable

Crystal PvP focused single-biome terrain generator for **Paper 1.21.11**.

Originally developed for **cpvpprac.eu**, a Crystal PvP practice server.
Demo / live server: **cpvpprac.eu**

---

## Compatibility

| Plugin Version | Server Software | Minecraft Version | Status               |
| -------------- | --------------- | ----------------- | -------------------- |
| 1.2.10         | Paper           | 1.21.11           | Stable / tested      |
| 1.3.x          | Paper           | newer versions    | Planned / not tested |

This plugin is currently built and tested for **Paper 1.21.11**.
Newer Paper/Minecraft versions are not officially supported until tested.

---

## Requirements

### Required

* Paper 1.21.11

### Optional integrations

| Plugin          |            Required | Purpose                                     |
| --------------- | ------------------: | ------------------------------------------- |
| Multiverse-Core | No, but recommended | World creation and arena management         |
| Chunky          | No, but recommended | Pregeneration after world creation or reset |

The core generator can create single-biome terrain without Chunky.

However, the intended production workflow uses:

* **Multiverse-Core** for creating, loading and managing arena worlds
* **Chunky** for pregenerating worlds after creation or reset

If **Multiverse-Core** is not installed, Multiverse-based world creation commands and workflows are not available.

If **Chunky** is not installed, Chunky-related commands and automatic pregeneration are not available.

---

## Features

* Custom single-biome worlds:

  * Plains
  * Desert
  * Badlands
  * Snow
  * Mushroom
  * End
* Open Crystal-PvP focused terrain generation
* PvP-friendly terrain readability with reduced clutter
* Configurable terrain height, noise, flatness and decoration density
* Biome-specific decoration configuration
* Sparse cinematic decoration system
* Automatic arena reset system
* Reset worlds automatically re-apply:

  * HARD difficulty
  * SURVIVAL gamemode
  * PvP enabled
  * configured world border
* Chunky pregeneration integration
* Multiverse-Core support
* Snow biome atmosphere:

  * light snowflake particles
  * thin snow layers
  * flat packed-ice ponds
  * sparse rocks
  * rare varied spruce trees
* Large mushroom structures for mushroom worlds

---

## Commands

### General

```text
/cpvpsb help
/cpvpsb reload
/cpvpsb biomes
/cpvpsb info
```

### World management

```text
/cpvpsb create <world> [biome]
/cpvpsb tp <world>
```

### Reset system

```text
/cpvpsb reset now
/cpvpsb reset <world>
/cpvpsb reset status
/cpvpsb reset reload
```

### Chunky integration

```text
/cpvpsb chunky start <world>
/cpvpsb chunky start-all
```

Chunky commands require Chunky to be installed.

---

## Multiverse usage

Create single-biome worlds with:

```text
/mv create plains normal -g CPVPSingleBiome:plains
/mv create desert normal -g CPVPSingleBiome:desert
/mv create badlands normal -g CPVPSingleBiome:badlands
/mv create snow normal -g CPVPSingleBiome:snow
/mv create mushroom normal -g CPVPSingleBiome:mushroom
/mv create end normal -g CPVPSingleBiome:end
```

These commands require Multiverse-Core.

---

## Available biome generators

```text
plains
desert
badlands
snow
mushroom
end
```

If no biome is specified, the plugin uses the configured `default-biome`.

---

## Configuration highlights

The plugin supports configuration for:

* default biome
* terrain height
* terrain variation
* noise scale
* flatness
* world border size
* decoration density
* biome-specific decoration overrides
* automatic reset interval
* reset time and timezone
* last full reset date
* Chunky pregeneration radius
* Snow biome atmosphere

Snow-specific options include:

* snowflake particles
* thin snow layers
* packed-ice ponds
* sparse rock details
* rare spruce trees

---

## Important notes

Existing chunks from older generator versions may cause terrain walls or visible chunk borders.

For best results after major terrain-generator changes, create fresh worlds or reset the affected arena world.

Chunky pregeneration is strongly recommended for production servers.

Arena worlds are designed for open Crystal PvP gameplay with reduced terrain clutter.
The goal is not vanilla realism, but readable PvP terrain with enough visual identity to keep worlds recognizable.

Since v1.2.6, reset worlds are forced back to HARD difficulty, SURVIVAL gamemode and PvP enabled after recreation. This prevents worlds from falling back to EASY after `/cpvpsb reset <world>`.

Since v1.2.8, the last full reset date is stored in the config so scheduled resets survive server restarts correctly.

Since v1.2.10, the Snow biome includes improved atmosphere, flatter packed-ice ponds and reduced visual clutter.

---

## Server

This plugin was originally developed for:

```text
cpvpprac.eu
```

A Crystal PvP practice server focused on open arena worlds and PvP readability.

---

## Disclaimer

This project is not affiliated with Mojang, Microsoft, Minecraft or PaperMC.
Minecraft is a trademark of Microsoft.

---
## License

GPL-3.0-only

