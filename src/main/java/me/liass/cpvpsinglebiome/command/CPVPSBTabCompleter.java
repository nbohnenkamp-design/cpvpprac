package me.liass.cpvpsinglebiome.command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.liass.cpvpsinglebiome.generator.BiomeType;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class CPVPSBTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS =
            Arrays.asList(
                    "help",
                    "reload",
                    "biomes",
                    "info",
                    "create",
                    "tp",
                    "reset",
                    "chunky"
            );

    private static final List<String> RESET_SUBS =
            Arrays.asList(
                    "now",
                    "status",
                    "reload"
            );

    private static final List<String> CHUNKY_SUBS =
            Arrays.asList(
                    "start",
                    "start-all"
            );

    private static final List<String> BIOME_NAMES =
            Arrays.stream(BiomeType.values())
                    .map(BiomeType::getId)
                    .collect(Collectors.toList());

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length == 1) {
            return filterPrefix(
                    SUBCOMMANDS,
                    args[0]
            );
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("tp")) {
                return filterPrefix(
                        getLoadedWorldNames(),
                        args[1]
                );
            }

            if (args[0].equalsIgnoreCase("create")) {
                return List.of();
            }

            if (args[0].equalsIgnoreCase("reset")) {
                return filterPrefix(
                        mergeLists(
                                RESET_SUBS,
                                getLoadedWorldNames()
                        ),
                        args[1]
                );
            }

            if (args[0].equalsIgnoreCase("chunky")) {
                return filterPrefix(
                        CHUNKY_SUBS,
                        args[1]
                );
            }

            return List.of();
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("create")) {
                return filterPrefix(
                        BIOME_NAMES,
                        args[2]
                );
            }

            if (args[0].equalsIgnoreCase("chunky")
                    && args[1].equalsIgnoreCase("start")) {
                return filterPrefix(
                        getLoadedWorldNames(),
                        args[2]
                );
            }

            return List.of();
        }

        return List.of();
    }

    private List<String> filterPrefix(
            List<String> options,
            String prefix
    ) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }

        String lowerPrefix =
                prefix == null
                        ? ""
                        : prefix.toLowerCase(Locale.ROOT);

        return options.stream()
                .filter(option -> option != null)
                .filter(option ->
                        option.toLowerCase(Locale.ROOT)
                                .startsWith(lowerPrefix)
                )
                .collect(Collectors.toList());
    }

    private List<String> getLoadedWorldNames() {
        return Bukkit.getWorlds()
                .stream()
                .map(World::getName)
                .collect(Collectors.toList());
    }

    private List<String> mergeLists(
            List<String> first,
            List<String> second
    ) {
        return Stream.concat(
                        first.stream(),
                        second.stream()
                )
                .distinct()
                .collect(Collectors.toList());
    }
}
