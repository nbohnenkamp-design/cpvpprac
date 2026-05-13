package eu.cpvpprac.practicebot;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the lifecycle of all active practice bots.
 * One bot per player; stored in two maps for O(1) look-up in both directions.
 */
public class BotManager {

    private final CPVPPracticeBotPlugin plugin;

    /** owner UUID → bot */
    private final Map<UUID, PracticeBot> bots = new HashMap<>();

    /** entity UUID → owner UUID — used by listeners to identify bot entities quickly */
    private final Map<UUID, UUID> entityOwnerMap = new HashMap<>();

    public BotManager(CPVPPracticeBotPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawns a new bot for the given player at their current location.
     * Any existing bot owned by this player is removed first.
     */
    public PracticeBot spawnBot(Player owner) {
        removeBot(owner.getUniqueId());

        ConfigManager cfg = plugin.getConfigManager();

        Husk husk = owner.getWorld().spawn(owner.getLocation(), Husk.class, entity -> {
            entity.setAI(false);                // Disable vanilla AI entirely — we drive it manually
            entity.setRemoveWhenFarAway(false);
            entity.setSilent(true);
            entity.setCustomNameVisible(true);
            entity.customName(MessageUtil.parse(cfg.botName));

            // Tag so listeners can identify this as a bot without iterating the map
            entity.getPersistentDataContainer().set(
                    plugin.getBotOwnerKey(),
                    PersistentDataType.STRING,
                    owner.getUniqueId().toString());

            ArmorUtil.applyArmor(entity, cfg);

            if (cfg.shieldEnabled) {
                ArmorUtil.applyShield(entity);
            }
        });

        PracticeBot bot = new PracticeBot(
                owner.getUniqueId(),
                husk,
                cfg.followEnabledByDefault,
                cfg.attackEnabledByDefault);

        bots.put(owner.getUniqueId(), bot);
        entityOwnerMap.put(husk.getUniqueId(), owner.getUniqueId());
        return bot;
    }

    /** Removes and despawns the bot owned by the given player (no-op if none). */
    public void removeBot(UUID ownerUuid) {
        PracticeBot bot = bots.remove(ownerUuid);
        if (bot == null) return;
        entityOwnerMap.remove(bot.getEntity().getUniqueId());
        if (bot.getEntity().isValid()) {
            bot.getEntity().remove();
        }
    }

    public PracticeBot getBot(UUID ownerUuid) {
        return bots.get(ownerUuid);
    }

    public boolean hasBot(UUID ownerUuid) {
        return bots.containsKey(ownerUuid);
    }

    /** Returns the owner UUID for a given entity, or null if it is not a bot. */
    public UUID getOwnerByEntity(Entity entity) {
        return entityOwnerMap.get(entity.getUniqueId());
    }

    public boolean isBotEntity(Entity entity) {
        return entityOwnerMap.containsKey(entity.getUniqueId());
    }

    /** Read-only view; safe to iterate on the main thread. */
    public Map<UUID, PracticeBot> getAllBots() {
        return Collections.unmodifiableMap(bots);
    }

    /** Despawn all active bots — called on plugin disable. */
    public void removeAllBots() {
        bots.values().forEach(bot -> {
            if (bot.getEntity().isValid()) {
                bot.getEntity().remove();
            }
        });
        bots.clear();
        entityOwnerMap.clear();
    }
}
