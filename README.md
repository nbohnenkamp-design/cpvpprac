# CPVPSingleBiome v1.0.0 Stable

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

---

## Requirements

### Required
- Paper 1.21.11

### Optional
- Multiverse-Core
- Chunky

---

## World Creation

Example:

```bash
/mv create plains normal -g CPVPSingleBiome:plains
```

### Available biome generators

- plains
- desert
- badlands
- snow
- mushroom
- end

---

## Commands

```bash
/cpvpsb reset now
/cpvpsb reset <world>
/cpvpsb reset reload
/cpvpsb chunky start <world>
/cpvpsb chunky start-all
```

---

## Notes

- Existing chunks from older generators may cause terrain walls or chunk borders.
- For best results, create fresh worlds after major terrain-generator updates.
- Chunky pregeneration is strongly recommended for production servers.
- Arena worlds are designed for open Crystal PvP gameplay with reduced terrain clutter.

---

## License

GPL-3.0-only
