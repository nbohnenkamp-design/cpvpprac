package eu.cpvpprac.practicebot;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Handles all /bot subcommands and provides tab-completion.
 *
 * Original subcommands (spawn, remove, tp, bring, attack, follow, reload)
 * are unchanged.  New additions: kit, kits.
 */
public class BotCommand implements CommandExecutor, TabCompleter {

    private final CPVPPracticeBotPlugin plugin;
    private final MessageUtil msg;

    public BotCommand(CPVPPracticeBotPlugin plugin) {
        this.plugin = plugin;
        this.msg    = new MessageUtil(plugin.getConfigManager());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if ("reload".equals(sub)) {
            if (!sender.hasPermission("cpvppracticebot.reload")) {
                msg.send(sender, "&cYou do not have permission to reload the configuration.");
                return true;
            }
            plugin.getConfigManager().reload();
            msg.send(sender, "&aConfiguration reloaded.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        if (!player.hasPermission("cpvppracticebot.use")) {
            msg.send(player, "&cYou do not have permission to use the practice bot.");
            return true;
        }

        switch (sub) {
            // ---- original subcommands (unchanged) ----
            case "spawn"  -> handleSpawn(player);
            case "remove" -> handleRemove(player);
            case "tp"     -> handleTp(player);
            case "bring"  -> handleBring(player);
            case "attack" -> handleToggle(player, args, false);
            case "follow" -> handleToggle(player, args, true);
            // ---- new subcommands ----
            case "kit"    -> handleKit(player, args);
            case "kits"   -> handleKits(player);
            default       -> sendHelp(player);
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Original handlers — unchanged
    // -------------------------------------------------------------------------

    private void handleSpawn(Player player) {
        boolean replacing = plugin.getBotManager().hasBot(player.getUniqueId());
        plugin.getBotManager().spawnBot(player);
        msg.send(player, replacing ? "&aReplaced your existing practice bot."
                                   : "&aYour practice bot has been spawned.");
    }

    private void handleRemove(Player player) {
        if (!plugin.getBotManager().hasBot(player.getUniqueId())) {
            msg.send(player, "&cYou do not have a practice bot.");
            return;
        }
        plugin.getBotManager().removeBot(player.getUniqueId());
        msg.send(player, "&aYour practice bot has been removed.");
    }

    private void handleTp(Player player) {
        PracticeBot bot = plugin.getBotManager().getBot(player.getUniqueId());
        if (bot == null) { msg.send(player, "&cYou do not have a practice bot."); return; }
        player.teleport(bot.getEntity().getLocation());
        msg.send(player, "&aTeleported to your practice bot.");
    }

    private void handleBring(Player player) {
        PracticeBot bot = plugin.getBotManager().getBot(player.getUniqueId());
        if (bot == null) { msg.send(player, "&cYou do not have a practice bot."); return; }
        bot.getEntity().teleport(player.getLocation());
        msg.send(player, "&aYour practice bot has been brought to you.");
    }

    /** Shared handler for /bot attack <on|off> and /bot follow <on|off>. */
    private void handleToggle(Player player, String[] args, boolean isFollow) {
        String modeName = isFollow ? "follow" : "attack";
        if (args.length < 2) { msg.send(player, "&cUsage: &5/bot " + modeName + " <on|off>"); return; }

        PracticeBot bot = plugin.getBotManager().getBot(player.getUniqueId());
        if (bot == null) { msg.send(player, "&cYou do not have a practice bot."); return; }

        boolean enable;
        if      ("on".equalsIgnoreCase(args[1]))  enable = true;
        else if ("off".equalsIgnoreCase(args[1])) enable = false;
        else { msg.send(player, "&cUsage: &5/bot " + modeName + " <on|off>"); return; }

        if (isFollow) bot.setFollowEnabled(enable);
        else          bot.setAttackEnabled(enable);

        msg.send(player, "&7" + capitalize(modeName) + " mode " + (enable ? "&aenabled" : "&cdisabled") + "&7.");
    }

    // -------------------------------------------------------------------------
    // New handlers
    // -------------------------------------------------------------------------

    /**
     * /bot kit <name>
     * Applies a named kit preset to the player's active bot immediately.
     */
    private void handleKit(Player player, String[] args) {
        if (args.length < 2) {
            msg.send(player, "&cUsage: &5/bot kit <name>  &7— use &5/bot kits &7to list available kits.");
            return;
        }

        PracticeBot bot = plugin.getBotManager().getBot(player.getUniqueId());
        if (bot == null) {
            msg.send(player, "&cYou do not have a practice bot. Use &5/bot spawn &cfirst.");
            return;
        }

        String kitName = args[1].toLowerCase();
        KitPreset kit  = plugin.getConfigManager().getKit(kitName);

        if (kit == null) {
            msg.send(player, "&cKit &5" + args[1] + "&c not found. Use &5/bot kits &cto see available kits.");
            return;
        }

        ArmorUtil.applyKit(bot.getEntity(), kit);
        bot.setCurrentKit(kit.getName());
        msg.send(player, "&aKit &5" + kit.getName() + "&a applied to your practice bot.");
    }

    /**
     * /bot kits
     * Lists all kit names defined in config with a one-line summary.
     */
    private void handleKits(Player player) {
        Map<String, KitPreset> kits = plugin.getConfigManager().getKits();

        if (kits.isEmpty()) {
            msg.send(player, "&cNo kits are defined in config.yml.");
            return;
        }

        String p = plugin.getConfigManager().prefix;
        player.sendMessage(MessageUtil.parse(p + "&7Available kits &8(&7" + kits.size() + " total&8):"));

        for (KitPreset kit : kits.values()) {
            String current = kit.getName().equals(
                    getBotKitName(player)) ? " &8[&acurrent&8]" : "";
            String summary = buildKitSummary(kit);
            player.sendMessage(MessageUtil.parse("  &5" + kit.getName() + current + " &8— &7" + summary));
        }

        player.sendMessage(MessageUtil.parse("  &8Use &5/bot kit <name> &8to apply."));
    }

    // -------------------------------------------------------------------------

    /** Returns the current kit name for the player's bot, or "" if no bot. */
    private String getBotKitName(Player player) {
        PracticeBot bot = plugin.getBotManager().getBot(player.getUniqueId());
        return bot != null ? bot.getCurrentKit() : "";
    }

    /** Builds a compact one-line description of a kit for the kits list. */
    private static String buildKitSummary(KitPreset kit) {
        StringBuilder sb = new StringBuilder();
        sb.append(titleCase(kit.getArmorMaterial())).append(" ")
          .append(friendlyEnchant(kit.getArmorEnchant()));

        if (!"NONE".equals(kit.getWeaponMaterial())) {
            sb.append(", ").append(friendlyWeapon(kit.getWeaponMaterial()));
            if (kit.getSharpnessLevel() > 0) sb.append(" S").append(kit.getSharpnessLevel());
        }
        if (kit.isShield()) sb.append(", Shield");
        if (!kit.getEffects().isEmpty()) sb.append(", +effects");
        return sb.toString();
    }

    private static String friendlyEnchant(String enchant) {
        return switch (enchant) {
            case "PROTECTION_4"       -> "Prot IV";
            case "BLAST_PROTECTION_4" -> "BProt IV";
            default                   -> enchant;
        };
    }

    private static String friendlyWeapon(String mat) {
        return switch (mat) {
            case "DIAMOND_SWORD"    -> "Diamond Sword";
            case "NETHERITE_SWORD"  -> "Netherite Sword";
            case "DIAMOND_AXE"      -> "Diamond Axe";
            case "NETHERITE_AXE"    -> "Netherite Axe";
            default                 -> titleCase(mat);
        };
    }

    private static String titleCase(String s) {
        if (s == null || s.isEmpty()) return s;
        String lower = s.replace("_", " ").toLowerCase();
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : lower.toCharArray()) {
            sb.append(nextUpper ? Character.toUpperCase(c) : c);
            nextUpper = c == ' ';
        }
        return sb.toString();
    }

    private void sendHelp(CommandSender sender) {
        String p = plugin.getConfigManager().prefix;
        sender.sendMessage(MessageUtil.parse(p + "&7Available commands:"));
        sender.sendMessage(MessageUtil.parse("  &5/bot spawn         &7- Spawn your practice bot"));
        sender.sendMessage(MessageUtil.parse("  &5/bot remove        &7- Remove your practice bot"));
        sender.sendMessage(MessageUtil.parse("  &5/bot tp            &7- Teleport to your bot"));
        sender.sendMessage(MessageUtil.parse("  &5/bot bring         &7- Bring your bot to you"));
        sender.sendMessage(MessageUtil.parse("  &5/bot attack on|off &7- Toggle attack mode"));
        sender.sendMessage(MessageUtil.parse("  &5/bot follow on|off &7- Toggle follow mode"));
        sender.sendMessage(MessageUtil.parse("  &5/bot kit <name>    &7- Apply an equipment kit to your bot"));
        sender.sendMessage(MessageUtil.parse("  &5/bot kits          &7- List all available kits"));
        sender.sendMessage(MessageUtil.parse("  &5/bot reload        &7- Reload configuration (requires permission)"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (args.length == 1)
            return Arrays.asList("spawn", "remove", "tp", "bring", "attack", "follow", "kit", "kits", "reload");

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ("attack".equals(sub) || "follow".equals(sub))
                return Arrays.asList("on", "off");
            if ("kit".equals(sub)) {
                List<String> names = new ArrayList<>(plugin.getConfigManager().getKits().keySet());
                Collections.sort(names);
                return names;
            }
        }
        return Collections.emptyList();
    }

    private static String capitalize(String s) {
        return (s == null || s.isEmpty()) ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
