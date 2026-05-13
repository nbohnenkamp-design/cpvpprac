package eu.cpvpprac.practicebot;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Flat cache of config.yml values.
 * Call {@link #reload()} to re-read the file at runtime.
 *
 * All original fields are preserved without modification.
 * Kit-related fields (defaultKit, kits) are new additions.
 */
public class ConfigManager {

    private final CPVPPracticeBotPlugin plugin;

    // -------------------------------------------------------------------------
    // Original fields — unchanged
    // -------------------------------------------------------------------------

    public String  botName;
    public String  armorType;
    public String  armorEnchantment;
    public boolean unlimitedHealth;
    public boolean antiKnockback;

    public boolean followEnabledByDefault;
    public double  followDistance;
    public double  followMovementSpeed;

    public boolean attackEnabledByDefault;
    public double  attackReach;
    public double  attackSpeed;          // attacks per second

    public boolean shieldEnabled;

    public String prefix;

    // -------------------------------------------------------------------------
    // Kit fields — new
    // -------------------------------------------------------------------------

    /** Name of the kit applied on /bot spawn.  Defaults to the first defined kit. */
    public String defaultKit;

    /** All named kits loaded from the kits: section. Never null; may be empty. */
    private Map<String, KitPreset> kits = new HashMap<>();

    // -------------------------------------------------------------------------

    public ConfigManager(CPVPPracticeBotPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        // Original values
        botName          = cfg.getString("bot.name", "&5Practice Bot");
        armorType        = cfg.getString("bot.armor.type", "DIAMOND").toUpperCase();
        armorEnchantment = cfg.getString("bot.armor.enchantment", "PROTECTION_4").toUpperCase();
        unlimitedHealth  = cfg.getBoolean("bot.unlimited-health", true);
        antiKnockback    = cfg.getBoolean("bot.anti-knockback", true);

        followEnabledByDefault = cfg.getBoolean("bot.follow.enabled-by-default", true);
        followDistance         = cfg.getDouble("bot.follow.distance", 3.0);
        followMovementSpeed    = cfg.getDouble("bot.follow.movement-speed", 0.35);

        attackEnabledByDefault = cfg.getBoolean("bot.attack.enabled-by-default", false);
        attackReach            = cfg.getDouble("bot.attack.reach", 3.0);
        attackSpeed            = cfg.getDouble("bot.attack.speed", 1.0);

        shieldEnabled = cfg.getBoolean("bot.shield.enabled", false);

        prefix = cfg.getString("messages.prefix", "&5[PracticeBot]&r ");

        // Kit loading
        kits = loadKits(cfg);
        String cfgDefault = cfg.getString("bot.default-kit", "");
        if (!cfgDefault.isEmpty() && kits.containsKey(cfgDefault)) {
            defaultKit = cfgDefault;
        } else if (!kits.isEmpty()) {
            defaultKit = kits.keySet().iterator().next();
        } else {
            defaultKit = "";
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Returns the KitPreset for the given name, or {@code null} if not found.
     * Name matching is case-insensitive.
     */
    public KitPreset getKit(String name) {
        if (name == null) return null;
        return kits.get(name.toLowerCase());
    }

    /** Returns an unmodifiable view of all loaded kits, keyed by lower-case name. */
    public Map<String, KitPreset> getKits() {
        return Collections.unmodifiableMap(kits);
    }

    // -------------------------------------------------------------------------

    private Map<String, KitPreset> loadKits(FileConfiguration cfg) {
        Map<String, KitPreset> result = new HashMap<>();
        ConfigurationSection kitsSection = cfg.getConfigurationSection("kits");
        if (kitsSection == null) return result;

        for (String rawName : kitsSection.getKeys(false)) {
            ConfigurationSection k = kitsSection.getConfigurationSection(rawName);
            if (k == null) continue;

            String name         = rawName.toLowerCase();
            String armor        = k.getString("armor", "DIAMOND").toUpperCase();
            String armorEnchant = k.getString("armor-enchant", "PROTECTION_4").toUpperCase();
            String weapon       = k.getString("weapon", "NONE").toUpperCase();
            int    sharpness    = k.getInt("sharpness", 0);
            boolean shield      = k.getBoolean("shield", false);
            int    totems       = k.getInt("totems", 0);

            Map<PotionEffectType, Integer> effects = new HashMap<>();
            ConfigurationSection effectsSec = k.getConfigurationSection("effects");
            if (effectsSec != null) {
                for (String effectName : effectsSec.getKeys(false)) {
                    PotionEffectType type = resolveEffectType(effectName);
                    if (type != null) {
                        effects.put(type, effectsSec.getInt(effectName, 0));
                    } else {
                        plugin.getLogger().warning(
                                "Unknown potion effect '" + effectName + "' in kit '" + name + "' — skipped.");
                    }
                }
            }

            result.put(name, new KitPreset(name, armor, armorEnchant, weapon, sharpness, shield, totems, effects));
        }
        return result;
    }

    /**
     * Resolves a config-supplied effect name to a PotionEffectType.
     * Tries the Bukkit registry first (preferred in 1.21), then falls back to
     * the legacy name-based lookup so old configs still work.
     */
    @SuppressWarnings("deprecation")
    private PotionEffectType resolveEffectType(String name) {
        // Try exact Bukkit name match (upper-case, e.g. "SPEED")
        PotionEffectType type = PotionEffectType.getByName(name.toUpperCase());
        if (type != null) return type;
        // Try lower-case variant (e.g. "speed") for flexibility
        return PotionEffectType.getByName(name.toLowerCase());
    }
}
