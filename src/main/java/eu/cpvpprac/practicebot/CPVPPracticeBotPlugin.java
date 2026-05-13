package eu.cpvpprac.practicebot;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for CPVPPracticeBot.
 * Initialises all subsystems and hooks them together on enable.
 */
public class CPVPPracticeBotPlugin extends JavaPlugin {

    /** PDC key used to tag bot entities with their owner's UUID string. */
    public static final String BOT_OWNER_KEY_ID = "bot_owner";

    private NamespacedKey botOwnerKey;
    private ConfigManager configManager;
    private BotManager botManager;
    private BotTask botTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        botOwnerKey    = new NamespacedKey(this, BOT_OWNER_KEY_ID);
        configManager  = new ConfigManager(this);
        botManager     = new BotManager(this);

        getServer().getPluginManager().registerEvents(new BotListener(this), this);

        BotCommand cmd = new BotCommand(this);
        getCommand("bot").setExecutor(cmd);
        getCommand("bot").setTabCompleter(cmd);

        // Single repeating task drives all bot AI (follow + attack)
        botTask = new BotTask(this);
        botTask.runTaskTimer(this, 1L, 1L);

        getLogger().info("CPVPPracticeBot enabled.");
    }

    @Override
    public void onDisable() {
        if (botTask != null) {
            botTask.cancel();
        }
        if (botManager != null) {
            botManager.removeAllBots();
        }
        getLogger().info("CPVPPracticeBot disabled.");
    }

    public NamespacedKey getBotOwnerKey() { return botOwnerKey; }
    public ConfigManager getConfigManager() { return configManager; }
    public BotManager getBotManager() { return botManager; }
}
