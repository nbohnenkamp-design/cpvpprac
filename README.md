# CPVPSingleBiome v1.2.6 Stable

Crystal PvP focused single-biome terrain generator for Paper 1.21.11.

---

## Features

- Custom Plains / Desert / Snow / Badlands / Mushroom worlds
- Open Crystal-PvP focused terrain generation
- Automatic arena reset system
- Chunky pregeneration integration
- Multiverse-Core support
- Sparse cinematic decoration system
- Configurable terrain height, flatness and decoration density
- Biome-specific decoration configuration
- Large mushroom structures
- Snow spruce generation
- PvP-focused open terrain readability

---

## Requirements

### Required

- Paper 1.21.11

### Recommended Plugins for Full Functionality

- Multiverse-Core
- Chunky

---

## Commands

### General

```bash
/cpvpsb help
/cpvpsb reload
/cpvpsb biomes
/cpvpsb info
```

### World Management

```bash
/cpvpsb create <world> [biome]
/cpvpsb tp <world>
```

### Reset System

```bash
/cpvpsb reset now
/cpvpsb reset <world>
/cpvpsb reset status
/cpvpsb reset reload
```

### Chunky Integration

```bash
/cpvpsb chunky start <world>
/cpvpsb chunky start-all
```

---

## Multiverse Usage

```bash
/mv create plains normal -g CPVPSingleBiome:plains
/mv create desert normal -g CPVPSingleBiome:desert
/mv create badlands normal -g CPVPSingleBiome:badlands
/mv create snow normal -g CPVPSingleBiome:snow
/mv create mushroom normal -g CPVPSingleBiome:mushroom
/mv create end normal -g CPVPSingleBiome:end
```

### Available biome generators

- plains
- desert
- badlands
- snow
- mushroom
- end

---

## Notes

- Existing chunks from older generators may cause terrain walls or chunk borders.
- For best results, create fresh worlds after major terrain-generator updates.
- Chunky pregeneration is strongly recommended for production servers.
- Arena worlds are designed for open Crystal PvP gameplay with reduced terrain clutter.

---

## License

GPL-3.0-only
