# CPVPPracticeBot

A simple, open-source Crystal PvP practice bot plugin for Paper 1.21.1.  
Built for **cpvpprac.eu / cpvpprac.net** as a maintainable replacement for closed-source alternatives.

No license system. No database. No packet NPCs.  
The bot is a real Bukkit entity (Husk) with configurable armour, follow, and attack behaviour.

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Minecraft / Paper | 1.21.1 |
| Java | 21 |

---

## Commands

All commands require the `cpvppracticebot.use` permission.

| Command | Description |
|---------|-------------|
| `/bot spawn` | Spawn a personal practice bot at your location (replaces existing bot) |
| `/bot remove` | Remove your practice bot |
| `/bot tp` | Teleport yourself to your bot |
| `/bot bring` | Teleport your bot to you |
| `/bot attack on\|off` | Toggle attack mode — the bot will periodically hit you |
| `/bot follow on\|off` | Toggle follow mode — the bot moves toward you |
| `/bot reload` | Reload `config.yml` (requires `cpvppracticebot.reload`) |

---

## Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `cpvppracticebot.use` | op | Spawn and control a personal bot |
| `cpvppracticebot.reload` | op | Reload the plugin configuration |

---

## Configuration (`config.yml`)

```yaml
bot:
  name: "&5Practice Bot"       # Display name (& colour codes supported)
  entity-type: HUSK            # Only HUSK is supported — daylight-immune by default

  armor:
    type: DIAMOND              # NONE | DIAMOND | NETHERITE
    enchantment: PROTECTION_4  # NONE | PROTECTION_4 | BLAST_PROTECTION_4

  unlimited-health: true       # Cancel all incoming damage (bot never dies)
  anti-knockback: true         # Cancel all velocity applied to the bot

  follow:
    enabled-by-default: true   # Follow mode active when bot spawns
    distance: 3.0              # Bot starts moving when further than this many blocks
    movement-speed: 0.35       # Blocks per tick toward the owner

  attack:
    enabled-by-default: false  # Attack mode inactive when bot spawns
    reach: 3.0                 # Maximum attack distance in blocks
    speed: 1.0                 # Attacks per second

  shield:
    enabled: false             # Give the bot a shield in its off-hand (visual only)

messages:
  prefix: "&5[PracticeBot]&r " # Chat prefix for all plugin messages
```

---

## Build Instructions

```bash
mvn clean package
```

Output jar: `target/CPVPPracticeBot-1.0.0.jar`

Drop it into your server's `plugins/` folder and restart.

---

## Design Notes

- One bot per player; the bot is removed automatically when the owner disconnects.
- All vanilla AI is disabled (`setAI(false)`); follow and attack are driven by a single Bukkit repeating task.
- No GUI, no database, no external dependencies beyond the Paper API.
- Husks are immune to daylight burning by default — no special handling required.
