package me.liass.cpvpsinglebiome.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import me.liass.cpvpsinglebiome.generator.BiomeType;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class CPVPSBTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "help",
            "version",
            "about",
            "status",
            "export",
            "reload",
            "biomes",
            "info",
            "create",
            "tp",
            "reset",
            "chunky"
    );

    private static final List<String> RESET_SUBS = List.of(
            "now",
            "status",
            "reload"
    );

    private static final List<String> CHUNKY_SUBS = List.of(
            "start",
            "start-all"
    );

    private static final List<String> BIOME_NAMES = buildBiomeNames();

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length == 1) {
            return filterPrefix(
                    visibleSubcommands(sender),
                    args[0]
            );
        }

        if (args.length == 2) {
            String first =
                    args[0].toLowerCase(Locale.ROOT);

            switch (first) {

                case "create":
                    if (!canUse(sender, "cpvpsinglebiome.create")) {
                        return Collections.emptyList();
                    }

                    return filterPrefix(
                            List.of("<worldname>"),
                            args[1]
                    );

                case "tp":
                    if (!canUse(sender, "cpvpsinglebiome.tp")) {
                        return Collections.emptyList();
                    }

                    return filterPrefix(
                            getLoadedWorldNames(),
                            args[1]
                    );

                case "reset":
                    if (!canUse(sender, "cpvpsinglebiome.reset")
                            && !canUse(sender, "cpvpsinglebiome.reset.now")) {
                        return Collections.emptyList();
                    }

                    return filterPrefix(
                            mergeLists(
                                    RESET_SUBS,
                                    getLoadedWorldNames()
                            ),
                            args[1]
                    );

                case "chunky":
                    if (!canUse(sender, "cpvpsinglebiome.chunky")) {
                        return Collections.emptyList();
                    }

                    return filterPrefix(
                            CHUNKY_SUBS,
                            args[1]
                    );

                default:
                    return Collections.emptyList();
            }
        }

        if (args.length == 3) {
            String first =
                    args[0].toLowerCase(Locale.ROOT);

            String second =
                    args[1].toLowerCase(Locale.ROOT);

            if ("create".equals(first)) {
                if (!canUse(sender, "cpvpsinglebiome.create")) {
                    return Collections.emptyList();
                }

                return filterPrefix(
                        BIOME_NAMES,
                        args[2]
                );
            }

            if ("chunky".equals(first)
                    && "start".equals(second)) {
                if (!canUse(sender, "cpvpsinglebiome.chunky")) {
                    return Collections.emptyList();
                }

                return filterPrefix(
                        getLoadedWorldNames(),
                        args[2]
                );
            }
        }

        return Collections.emptyList();
    }

    private List<String> visibleSubcommands(CommandSender sender) {
        List<String> result = new ArrayList<>();

        for (String subcommand : SUBCOMMANDS) {
            String permission =
                    permissionForSubcommand(subcommand);

            if (permission == null || canUse(sender, permission)) {
                result.add(subcommand);
            }
        }

        return result;
    }

    private String permissionForSubcommand(String subcommand) {
        switch (subcommand) {

            case "version":
                return "cpvpsinglebiome.version";

            case "about":
                return "cpvpsinglebiome.about";

            case "status":
                return "cpvpsinglebiome.status";

            case "export":
                return "cpvpsinglebiome.export";

            case "reload":
                return "cpvpsinglebiome.reload";

            case "biomes":
            case "info":
                return "cpvpsinglebiome.info";

            case "create":
                return "cpvpsinglebiome.create";

            case "tp":
                return "cpvpsinglebiome.tp";

            case "reset":
                return "cpvpsinglebiome.reset";

            case "chunky":
                return "cpvpsinglebiome.chunky";

            case "help":
            default:
                return null;
        }
    }

    private boolean canUse(
            CommandSender sender,
            String permission
    ) {
        return sender.hasPermission("cpvpsinglebiome.admin")
                || sender.hasPermission(permission);
    }

    private static List<String> buildBiomeNames() {
        List<String> names = new ArrayList<>();

        for (BiomeType type : BiomeType.values()) {
            names.add(type.getId());
        }

        return Collections.unmodifiableList(names);
    }

    private List<String> filterPrefix(
            List<String> input,
            String prefix
    ) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }

        String lowerPrefix =
                prefix == null
                        ? ""
                        : prefix.toLowerCase(Locale.ROOT);

        List<String> result = new ArrayList<>();

        for (String value : input) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                result.add(value);
            }
        }

        return result;
    }

    private List<String> getLoadedWorldNames() {
        List<String> names = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            names.add(world.getName());
        }

        Collections.sort(names);

        return names;
    }

    private List<String> mergeLists(
            List<String> first,
            List<String> second
    ) {
        List<String> result = new ArrayList<>();

        if (first != null) {
            result.addAll(first);
        }

        if (second != null) {
            for (String value : second) {
                if (!result.contains(value)) {
                    result.add(value);
                }
            }
        }

        return result;
    }
}
