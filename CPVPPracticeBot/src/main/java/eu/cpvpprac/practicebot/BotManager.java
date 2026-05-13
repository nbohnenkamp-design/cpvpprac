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
 * One bot per player; two maps give O(1) lookup in both directions.
 *
 * Change from original: spawnBot() now applies the default kit via
 * ArmorUtil.applyKit() instead of ArmorUtil.applyArmor() + applyShield().
 * All other methods are unchanged.
 */
public class BotManager {

    private final CPVPPracticeBotPlugin plugin;

    /** owner UUID → bot */
    private final Map<UUID, PracticeBot> bots = new HashMap<>();

    /** entity UUID → owner UUID  (used by event listeners) */
    private final Map<UUID, UUID> entityOwnerMap = new HashMap<>();

    public BotManager(CPVPPracticeBotPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawns a new bot for the given player at their current location.
     * Any pre-existing bot owned by this player is removed first.
     *
     * Equipment is applied via the default kit from config.  If no valid kit
     * is configured the original applyArmor + applyShield fallback is used so
     * the plugin always produces a usable bot regardless of config state.
     */
    public PracticeBot spawnBot(Player owner) {
        removeBot(owner.getUniqueId());

        ConfigManager cfg     = plugin.getConfigManager();
        KitPreset     defKit  = cfg.getKit(cfg.defaultKit);

        Husk husk = owner.getWorld().spawn(owner.getLocation(), Husk.class, entity -> {
            entity.setAI(false);
            entity.setRemoveWhenFarAway(false);
            entity.setSilent(true);
            entity.setCustomNameVisible(true);
            entity.customName(MessageUtil.parse(cfg.botName));

            entity.getPersistentDataContainer().set(
                    plugin.getBotOwnerKey(),
                    PersistentDataType.STRING,
                    owner.getUniqueId().toString());

            if (defKit != null) {
                ArmorUtil.applyKit(entity, defKit);
            } else {
                // Fallback: use the original flat-config armor approach
                ArmorUtil.applyArmor(entity, cfg);
                if (cfg.shieldEnabled) ArmorUtil.applyShield(entity);
            }
        });

        PracticeBot bot = new PracticeBot(
                owner.getUniqueId(), husk,
                cfg.followEnabledByDefault,
                cfg.attackEnabledByDefault);

        if (defKit != null) {
            bot.setCurrentKit(defKit.getName());
        }

        bots.put(owner.getUniqueId(), bot);
        entityOwnerMap.put(husk.getUniqueId(), owner.getUniqueId());
        return bot;
    }

    /** Removes and despawns the bot owned by the given player (no-op if none). */
    public void removeBot(UUID ownerUuid) {
        PracticeBot bot = bots.remove(ownerUuid);
        if (bot == null) return;
        entityOwnerMap.remove(bot.getEntity().getUniqueId());
        if (bot.getEntity().isValid()) bot.getEntity().remove();
    }

    public PracticeBot getBot(UUID ownerUuid)    { return bots.get(ownerUuid); }
    public boolean     hasBot(UUID ownerUuid)    { return bots.containsKey(ownerUuid); }

    /** Returns the owner UUID for a given entity, or null if not a bot. */
    public UUID    getOwnerByEntity(Entity entity) { return entityOwnerMap.get(entity.getUniqueId()); }
    public boolean isBotEntity(Entity entity)      { return entityOwnerMap.containsKey(entity.getUniqueId()); }

    public Map<UUID, PracticeBot> getAllBots() { return Collections.unmodifiableMap(bots); }

    /** Despawn all active bots — called on plugin disable. */
    public void removeAllBots() {
        bots.values().forEach(bot -> { if (bot.getEntity().isValid()) bot.getEntity().remove(); });
        bots.clear();
        entityOwnerMap.clear();
    }
}
