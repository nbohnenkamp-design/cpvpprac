package eu.cpvpprac.practicebot;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Flat cache of config.yml values.
 * Call {@link #reload()} after the underlying FileConfiguration is refreshed.
 */
public class ConfigManager {

    private final CPVPPracticeBotPlugin plugin;

    // Bot appearance
    public String botName;

    // Armor
    public String armorType;
    public String armorEnchantment;

    // Combat settings
    public boolean unlimitedHealth;
    public boolean antiKnockback;

    // Follow
    public boolean followEnabledByDefault;
    public double  followDistance;
    public double  followMovementSpeed;

    // Attack
    public boolean attackEnabledByDefault;
    public double  attackReach;
    public double  attackSpeed;          // attacks per second

    // Shield
    public boolean shieldEnabled;

    // Messages
    public String prefix;

    public ConfigManager(CPVPPracticeBotPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        botName            = cfg.getString("bot.name", "&5Practice Bot");
        armorType          = cfg.getString("bot.armor.type", "DIAMOND").toUpperCase();
        armorEnchantment   = cfg.getString("bot.armor.enchantment", "PROTECTION_4").toUpperCase();
        unlimitedHealth    = cfg.getBoolean("bot.unlimited-health", true);
        antiKnockback      = cfg.getBoolean("bot.anti-knockback", true);

        followEnabledByDefault = cfg.getBoolean("bot.follow.enabled-by-default", true);
        followDistance         = cfg.getDouble("bot.follow.distance", 3.0);
        followMovementSpeed    = cfg.getDouble("bot.follow.movement-speed", 0.35);

        attackEnabledByDefault = cfg.getBoolean("bot.attack.enabled-by-default", false);
        attackReach            = cfg.getDouble("bot.attack.reach", 3.0);
        attackSpeed            = cfg.getDouble("bot.attack.speed", 1.0);

        shieldEnabled = cfg.getBoolean("bot.shield.enabled", false);

        prefix = cfg.getString("messages.prefix", "&5[PracticeBot]&r ");
    }
}
