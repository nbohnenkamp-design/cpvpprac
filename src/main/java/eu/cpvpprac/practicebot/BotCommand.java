package eu.cpvpprac.practicebot;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handles all /bot subcommands and provides tab-completion.
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
            case "spawn"  -> handleSpawn(player);
            case "remove" -> handleRemove(player);
            case "tp"     -> handleTp(player);
            case "bring"  -> handleBring(player);
            case "attack" -> handleToggle(player, args, false);
            case "follow" -> handleToggle(player, args, true);
            default       -> sendHelp(player);
        }

        return true;
    }

    // -------------------------------------------------------------------------

    private void handleSpawn(Player player) {
        boolean replacing = plugin.getBotManager().hasBot(player.getUniqueId());
        plugin.getBotManager().spawnBot(player);
        if (replacing) {
            msg.send(player, "&aReplaced your existing practice bot.");
        } else {
            msg.send(player, "&aYour practice bot has been spawned.");
        }
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
        if (bot == null) {
            msg.send(player, "&cYou do not have a practice bot.");
            return;
        }
        player.teleport(bot.getEntity().getLocation());
        msg.send(player, "&aTeleported to your practice bot.");
    }

    private void handleBring(Player player) {
        PracticeBot bot = plugin.getBotManager().getBot(player.getUniqueId());
        if (bot == null) {
            msg.send(player, "&cYou do not have a practice bot.");
            return;
        }
        bot.getEntity().teleport(player.getLocation());
        msg.send(player, "&aYour practice bot has been brought to you.");
    }

    /**
     * Shared handler for /bot attack and /bot follow.
     * @param isFollow true for follow mode, false for attack mode
     */
    private void handleToggle(Player player, String[] args, boolean isFollow) {
        String modeName = isFollow ? "follow" : "attack";

        if (args.length < 2) {
            msg.send(player, "&cUsage: &5/bot " + modeName + " <on|off>");
            return;
        }

        PracticeBot bot = plugin.getBotManager().getBot(player.getUniqueId());
        if (bot == null) {
            msg.send(player, "&cYou do not have a practice bot.");
            return;
        }

        boolean enable;
        if ("on".equalsIgnoreCase(args[1])) {
            enable = true;
        } else if ("off".equalsIgnoreCase(args[1])) {
            enable = false;
        } else {
            msg.send(player, "&cUsage: &5/bot " + modeName + " <on|off>");
            return;
        }

        if (isFollow) {
            bot.setFollowEnabled(enable);
        } else {
            bot.setAttackEnabled(enable);
        }

        String state = enable ? "&aenabled" : "&cdisabled";
        msg.send(player, "&7" + capitalize(modeName) + " mode " + state + "&7.");
    }

    private void sendHelp(CommandSender sender) {
        String p = plugin.getConfigManager().prefix;
        sender.sendMessage(MessageUtil.parse(p + "&7Available commands:"));
        sender.sendMessage(MessageUtil.parse("  &5/bot spawn        &7- Spawn your practice bot"));
        sender.sendMessage(MessageUtil.parse("  &5/bot remove       &7- Remove your practice bot"));
        sender.sendMessage(MessageUtil.parse("  &5/bot tp           &7- Teleport to your bot"));
        sender.sendMessage(MessageUtil.parse("  &5/bot bring        &7- Bring your bot to you"));
        sender.sendMessage(MessageUtil.parse("  &5/bot attack on|off &7- Toggle attack mode"));
        sender.sendMessage(MessageUtil.parse("  &5/bot follow on|off &7- Toggle follow mode"));
        sender.sendMessage(MessageUtil.parse("  &5/bot reload       &7- Reload configuration (requires permission)"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("spawn", "remove", "tp", "bring", "attack", "follow", "reload");
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ("attack".equals(sub) || "follow".equals(sub)) {
                return Arrays.asList("on", "off");
            }
        }
        return Collections.emptyList();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
