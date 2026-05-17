package me.liass.cpvpsinglebiome.chunky;

import java.util.List;

import me.liass.cpvpsinglebiome.config.ConfigManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;

public final class ChunkyIntegration {

    private static final String CHUNKY_PLUGIN = "Chunky";

    private ChunkyIntegration() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin(CHUNKY_PLUGIN) != null;
    }

    public static boolean start(
            Plugin plugin,
            World world,
            ConfigManager config
    ) {
        int cx;
        int cz;

        if (world == null) {
            plugin.getLogger().warning(
                    "Chunky start aborted: world is null."
            );
            return false;
        }

        if (!isAvailable()) {
            plugin.getLogger().warning(
                    "Chunky is not installed — skipping pre-generation for '"
                            + world.getName()
                            + "'."
            );
            return false;
        }

        ConsoleCommandSender console = Bukkit.getConsoleSender();

        String name = world.getName();
        int radius = config.getChunkyRadius();
        String shape = config.getChunkyShape();

        if (config.isChunkyCenterSpawn()) {
            Location spawn = world.getSpawnLocation();

            cx = spawn.getBlockX();
            cz = spawn.getBlockZ();
        } else {
            cx = 0;
            cz = 0;
        }

        Bukkit.dispatchCommand(
                (CommandSender) console,
                "chunky world " + name
        );

        Bukkit.dispatchCommand(
                (CommandSender) console,
                "chunky center " + cx + " " + cz
        );

        Bukkit.dispatchCommand(
                (CommandSender) console,
                "chunky radius " + radius
        );

        Bukkit.dispatchCommand(
                (CommandSender) console,
                "chunky shape " + shape
        );

        if (config.isChunkySilent()) {
            Bukkit.dispatchCommand(
                    (CommandSender) console,
                    "chunky quiet on"
            );
        }

        Bukkit.dispatchCommand(
                (CommandSender) console,
                "chunky start"
        );

        plugin.getLogger().info(
                "Chunky pre-generation started for world '"
                        + name
                        + "' (shape="
                        + shape
                        + ", radius="
                        + radius
                        + ", center="
                        + cx
                        + "/"
                        + cz
                        + ")."
        );

        return true;
    }

    public static void startAll(
            Plugin plugin,
            List<String> worldNames,
            ConfigManager config
    ) {
        if (!isAvailable()) {
            plugin.getLogger().warning(
                    "Chunky is not installed — skipping bulk pre-generation."
            );
            return;
        }

        long delayTicks =
                Math.max(1, config.getChunkyDelaySeconds()) * 20L;

        int i = 0;

        for (String name : worldNames) {
            String wn = name;

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> {
                        World world = Bukkit.getWorld(wn);

                        if (world == null) {
                            plugin.getLogger().warning(
                                    "Chunky bulk start: world '"
                                            + wn
                                            + "' not loaded."
                            );
                            return;
                        }

                        start(plugin, world, config);
                    },
                    i * delayTicks
            );

            i++;
        }
    }
}
