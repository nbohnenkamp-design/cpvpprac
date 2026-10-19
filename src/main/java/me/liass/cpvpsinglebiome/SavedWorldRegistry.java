package me.liass.cpvpsinglebiome;

import me.liass.cpvpsinglebiome.config.ConfigManager;
import me.liass.cpvpsinglebiome.generator.BiomeType;
import me.liass.cpvpsinglebiome.generator.SingleBiomeChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SavedWorldRegistry {
    private final CPVPSingleBiomePlugin plugin;
    private final ConfigManager config;
    private final File file;

    public SavedWorldRegistry(CPVPSingleBiomePlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.file = new File(plugin.getDataFolder(), "worlds.yml");
    }

    private boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9_.-]{1,64}")
                && !name.equals(".") && !name.equals("..");
    }

    public void remember(String name, String biome) {
        if (!validName(name) || BiomeType.fromString(biome) == null) {
            plugin.getLogger().warning("World not registered for autoload: " + name);
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Map<?, ?> entry : yaml.getMapList("worlds")) {
            if (!name.equalsIgnoreCase(String.valueOf(entry.get("name")))) {
                Map<String, Object> saved = new LinkedHashMap<>();
                saved.put("name", entry.get("name"));
                saved.put("biome", entry.get("biome"));
                entries.add(saved);
            }
        }
        Map<String, Object> added = new LinkedHashMap<>();
        added.put("name", name);
        added.put("biome", biome);
        entries.add(added);
        yaml.set("worlds", entries);
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
            plugin.getLogger().info("Registered world for autoload: " + name + " (" + biome + ")");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save worlds.yml: " + e.getMessage());
        }
    }

    // Retries only while an installed Multiverse is still starting.
    private int mvStartRetries = 0;

    private record SavedWorld(String name, BiomeType biome) {}

    public void loadExisting() {
        if (!file.isFile()) {
            return;
        }

        org.bukkit.plugin.Plugin mv = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        if (mv != null && !mv.isEnabled()) {
            if (++mvStartRetries <= 20) {
                Bukkit.getScheduler().runTaskLater(plugin, this::loadExisting, 20L);
            } else {
                plugin.getLogger().warning("Multiverse is installed but not enabled."
                        + " Autoload skipped to avoid conflicts.");
            }
            return;
        }

        // Paper and Multiverse are done with their basic startup by this point.
        World primary = Bukkit.getWorlds().stream()
                .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                .findFirst().orElse(null);
        if (primary == null) {
            plugin.getLogger().warning("Autoload skipped: primary world is not loaded.");
            return;
        }

        Path container = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path modernRoot = container.resolve(primary.getName())
                .resolve("dimensions").resolve("minecraft").normalize();

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<SavedWorld> existing = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Map<?, ?> entry : yaml.getMapList("worlds")) {
            Object rawName = entry.get("name");
            Object rawBiome = entry.get("biome");
            if (!(rawName instanceof String name) || !(rawBiome instanceof String biomeName)) {
                plugin.getLogger().warning("Invalid worlds.yml entry skipped.");
                continue;
            }
            BiomeType biome = BiomeType.fromString(biomeName);
            if (!validName(name) || biome == null
                    || !seen.add(name.toLowerCase(Locale.ROOT))
                    || Bukkit.getWorld(name) != null) {
                continue;
            }
            Path legacy = container.resolve(name).normalize();
            Path modern = modernRoot.resolve(name.toLowerCase(Locale.ROOT)).normalize();
            if (!legacy.startsWith(container) || !modern.startsWith(modernRoot)) {
                continue;
            }
            // Never import or create worlds for missing data folders.
            boolean oldData = Files.isRegularFile(legacy.resolve("level.dat"))
                    || Files.isDirectory(legacy.resolve("region"));
            boolean newData = Files.isDirectory(modern.resolve("region"))
                    || Files.isRegularFile(modern.resolve("paper-world.yml"))
                    || Files.isDirectory(modern.resolve("data"));
            if (!oldData && !newData) {
                plugin.getLogger().warning("Not loading missing world (no regeneration): " + name);
                continue;
            }
            existing.add(new SavedWorld(name, biome));
        }
        if (existing.isEmpty()) {
            return;
        }

        if (mv != null && mv.isEnabled()) {
            // First verify the Multiverse 5.x API is ready; do not blindly import
            // worlds that may already be in Multiverse's registry.
            if (knownToMultiverse(mv, existing.get(0).name()) == null) {
                if (++mvStartRetries <= 20) {
                    Bukkit.getScheduler().runTaskLater(plugin, this::loadExisting, 20L);
                } else {
                    plugin.getLogger().warning("Multiverse API unavailable. Autoload skipped;"
                            + " use /mv import with the correct generator manually.");
                }
                return;
            }
            mvStartRetries = 0;
            loadNextViaMultiverse(existing, 0, mv);
        } else {
            // Standalone path: unchanged use of Bukkit/Paper WorldCreator.
            for (SavedWorld spec : existing) {
                try {
                    WorldCreator creator = new WorldCreator(spec.name());
                    creator.generator(new SingleBiomeChunkGenerator(config, spec.biome()));
                    creator.generateStructures(false);
                    World loaded = creator.createWorld();
                    if (loaded == null) {
                        plugin.getLogger().warning("Autoload returned null for world: " + spec.name());
                    } else {
                        plugin.getLogger().info("Autoloaded world: " + spec.name()
                                + " (" + spec.biome().getId() + ")");
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Autoload failed for " + spec.name() + ": " + e);
                }
            }
        }
    }

    // Uses reflection so Multiverse remains an optional dependency in pom.xml.
    // null means API unavailable, true/false means known/unknown to Multiverse.
    private Boolean knownToMultiverse(org.bukkit.plugin.Plugin mv, String name) {
        try {
            Class<?> apiClass = Class.forName(
                    "org.mvplugins.multiverse.core.MultiverseCoreApi",
                    false, mv.getClass().getClassLoader());
            if (!Boolean.TRUE.equals(apiClass.getMethod("isLoaded").invoke(null))) {
                return null;
            }
            Object api = apiClass.getMethod("get").invoke(null);
            Object manager = apiClass.getMethod("getWorldManager").invoke(api);
            return (Boolean) manager.getClass().getMethod("isWorld", String.class)
                    .invoke(manager, name);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            plugin.getLogger().warning("Could not inspect Multiverse registry for '"
                    + name + "': " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    private void loadNextViaMultiverse(
            List<SavedWorld> worlds, int index, org.bukkit.plugin.Plugin mv) {
        if (!plugin.isEnabled() || index >= worlds.size()) {
            return;
        }
        SavedWorld spec = worlds.get(index);
        String name = spec.name();
        if (Bukkit.getWorld(name) != null) {
            loadNextViaMultiverse(worlds, index + 1, mv);
            return;
        }

        Boolean registered = knownToMultiverse(mv, name);
        if (registered == null) {
            plugin.getLogger().warning("Skipping '" + name
                    + "': Multiverse registration status could not be determined.");
            loadNextViaMultiverse(worlds, index + 1, mv);
            return;
        }

        String command = registered
                ? "mv load " + name
                : "mv import " + name + " normal --generator CPVPSingleBiome:"
                        + spec.biome().getId();
        plugin.getLogger().info("Restoring world with Multiverse: " + command);
        if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
            plugin.getLogger().warning("Could not dispatch Multiverse command for: " + name);
            loadNextViaMultiverse(worlds, index + 1, mv);
            return;
        }

        // Multiverse may finish loading asynchronously, as observed in 26.3.
        // Handle worlds one at a time to avoid concurrent heavy chunk loading.
        final int[] attempts = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (Bukkit.getWorld(name) != null) {
                task.cancel();
                Boolean nowRegistered = knownToMultiverse(mv, name);
                if (Boolean.TRUE.equals(nowRegistered)) {
                    plugin.getLogger().info("Autoloaded via Multiverse: " + name
                            + " (" + spec.biome().getId() + ")");
                } else {
                    plugin.getLogger().warning("World loaded, but Multiverse registration"
                            + " was not verified: " + name);
                }
                loadNextViaMultiverse(worlds, index + 1, mv);
            } else if (++attempts[0] >= 90) {
                task.cancel();
                plugin.getLogger().warning("Multiverse did not load '" + name
                        + "' within 90 seconds; check server logs.");
                loadNextViaMultiverse(worlds, index + 1, mv);
            }
        }, 20L, 20L);
    }
}
