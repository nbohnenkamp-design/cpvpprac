package me.liass.cpvpsinglebiome.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.liass.cpvpsinglebiome.CPVPSingleBiomePlugin;
import me.liass.cpvpsinglebiome.reset.ResetWorldSpec;

import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final CPVPSingleBiomePlugin plugin;
    private FileConfiguration config;

    public ConfigManager(CPVPSingleBiomePlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();
    }

    public String getDefaultBiome() {
        return this.config.getString("default-biome", "desert");
    }

    public int getBaseHeight() {
        if (!this.config.contains("base-height")
                && this.config.contains("terrain-height")) {
            return this.config.getInt("terrain-height", 70);
        }

        return this.config.getInt("base-height", 70);
    }

    public double getHeightVariation() {
        return Math.max(
                0.0D,
                this.config.getDouble("height-variation", 10.0D)
        );
    }

    public double getNoiseScale() {
        double v = this.config.getDouble("noise-scale", 80.0D);

        return (v <= 0.0D) ? 80.0D : v;
    }

    public double getFlatness() {
        double v = this.config.getDouble("flatness", 0.35D);

        if (v < 0.0D) {
            return 0.0D;
        }

        if (v > 1.0D) {
            return 1.0D;
        }

        return v;
    }

    public double getWorldBorderSize() {
        return this.config.getDouble("world-border-size", 0.0D);
    }

    public String getPrefix() {
        return this.config.getString(
                "messages-prefix",
                "§5[§dCPVPSB§5] §d"
        );
    }

    public boolean isDecorationEnabled() {
        return this.config.getBoolean("decoration.enabled", true);
    }

    public double getDecorationDensity() {
        return clampDensity(
                this.config.getDouble("decoration.density", 0.15D)
        );
    }

    public double getBiomeDecorationDensity(String biome) {
        String path =
                "decoration.biomes."
                        + biome.toLowerCase(Locale.ROOT)
                        + ".density";

        if (this.config.contains(path)) {
            return clampDensity(this.config.getDouble(path));
        }

        return getDecorationDensity();
    }

    public double getTreeDensity() {
        return clampTreeDensity(
                this.config.getDouble("decoration.tree-density", 0.4D)
        );
    }

    public double getBiomeTreeDensity(String biome) {
        String path =
                "decoration.biomes."
                        + biome.toLowerCase(Locale.ROOT)
                        + ".tree-density";

        if (this.config.contains(path)) {
            return clampTreeDensity(this.config.getDouble(path));
        }

        return getTreeDensity();
    }

    public boolean isFeatureEnabled(String feature) {
        return this.config.getBoolean(
                "decoration." + feature,
                true
        );
    }

    private double clampDensity(double v) {
        if (v < 0.0D) {
            return 0.0D;
        }

        if (v > 1.0D) {
            return 1.0D;
        }

        return v;
    }

    private double clampTreeDensity(double v) {
        if (v < 0.0D) {
            return 0.0D;
        }

        if (v > 2.0D) {
            return 2.0D;
        }

        return v;
    }

    private double clampProbability(double v) {
        if (v < 0.0D) {
            return 0.0D;
        }

        if (v > 1.0D) {
            return 1.0D;
        }

        return v;
    }

    public boolean isSnowflakeParticlesEnabled() {
        return this.config.getBoolean(
                "snow-effects.snowflake-particles.enabled",
                true
        );
    }

    public int getSnowflakeParticleIntervalTicks() {
        int v =
                this.config.getInt(
                        "snow-effects.snowflake-particles.interval-ticks",
                        20
                );

        return Math.max(5, v);
    }

    public int getSnowflakeParticlesPerPlayer() {
        int v =
                this.config.getInt(
                        "snow-effects.snowflake-particles.particles-per-player",
                        5
                );

        return Math.max(0, Math.min(50, v));
    }

    public double getSnowflakeParticleRadius() {
        double v =
                this.config.getDouble(
                        "snow-effects.snowflake-particles.radius",
                        10.0D
                );

        if (v < 1.0D) {
            return 1.0D;
        }

        if (v > 48.0D) {
            return 48.0D;
        }

        return v;
    }

    public double getSnowflakeParticleHeight() {
        double v =
                this.config.getDouble(
                        "snow-effects.snowflake-particles.height",
                        4.0D
                );

        if (v < 1.0D) {
            return 1.0D;
        }

        if (v > 24.0D) {
            return 24.0D;
        }

        return v;
    }

    public boolean isSnowLayersEnabled() {
        return this.config.getBoolean(
                "snow-effects.terrain.snow-layers",
                true
        );
    }

    public double getSnowLayerChance() {
        return clampProbability(
                this.config.getDouble(
                        "snow-effects.terrain.snow-layer-chance",
                        0.55D
                )
        );
    }

    public int getSnowMaxLayerHeight() {
        int v =
                this.config.getInt(
                        "snow-effects.terrain.max-snow-layer-height",
                        1
                );

        return Math.max(1, Math.min(8, v));
    }

    public boolean isSnowIceDetailsEnabled() {
        return this.config.getBoolean(
                "snow-effects.ice-details.enabled",
                true
        );
    }

    public double getSnowPondChance() {
        return clampProbability(
                this.config.getDouble(
                        "snow-effects.ice-details.pond-chance",
                        0.005D
                )
        );
    }

    public double getSnowSmallIcePatchChance() {
        return clampProbability(
                this.config.getDouble(
                        "snow-effects.ice-details.small-ice-patch-chance",
                        0.02D
                )
        );
    }

    public int getSnowMaxPondRadius() {
        int v =
                this.config.getInt(
                        "snow-effects.ice-details.max-pond-radius",
                        3
                );

        return Math.max(1, Math.min(8, v));
    }

    public int getSnowMaxIcePatchRadius() {
        int v =
                this.config.getInt(
                        "snow-effects.ice-details.max-ice-patch-radius",
                        3
                );

        return Math.max(1, Math.min(8, v));
    }

    public boolean isSnowBlueIceEnabled() {
        return this.config.getBoolean(
                "snow-effects.ice-details.use-blue-ice",
                false
        );
    }

    public boolean isSnowRocksEnabled() {
        return this.config.getBoolean(
                "snow-effects.rocks.enabled",
                true
        );
    }

    public double getSnowRockChance() {
        return clampProbability(
                this.config.getDouble(
                        "snow-effects.rocks.chance",
                        0.025D
                )
        );
    }

    public int getSnowRockMaxSize() {
        int v =
                this.config.getInt(
                        "snow-effects.rocks.max-size",
                        4
                );

        return Math.max(1, Math.min(8, v));
    }

    public boolean isSnowTreesEnabled() {
        return this.config.getBoolean(
                "snow-effects.trees.enabled",
                true
        );
    }

    public double getSnowTreeChance() {
        return clampProbability(
                this.config.getDouble(
                        "snow-effects.trees.chance",
                        0.04D
                )
        );
    }

    public boolean isResetEnabled() {
        return this.config.getBoolean("reset.enabled", false);
    }

    public LocalTime getResetTime() {
        String raw = this.config.getString("reset.time", "05:00");

        try {
            return LocalTime.parse(raw);
        } catch (Exception e) {
            return LocalTime.of(5, 0);
        }
    }

    public ZoneId getResetZone() {
        String raw = this.config.getString("reset.timezone", "");

        if (raw == null || raw.isEmpty()) {
            return ZoneId.systemDefault();
        }

        try {
            return ZoneId.of(raw);
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }

    public String getFallbackWorld() {
        return this.config.getString(
                "reset.fallback-world",
                "world"
        );
    }

    public boolean isSkipIfAnyPlayerOnline() {
        return this.config.getBoolean(
                "reset.skip-if-any-player-online",
                false
        );
    }

    public int getRetryAfterMinutes() {
        int v = this.config.getInt(
                "reset.retry-after-minutes",
                10
        );

        return Math.max(1, v);
    }

    public boolean isBlockJoinsDuringReset() {
        return this.config.getBoolean(
                "reset.block-joins-during-reset",
                true
        );
    }

    public boolean isBlockJoinsDuringChunky() {
        return this.config.getBoolean(
                "reset.block-joins-during-chunky",
                false
        );
    }

    public String getMaintenanceBypassPermission() {
        return this.config.getString(
                "reset.maintenance-bypass-permission",
                "cpvpsinglebiome.maintenance.bypass"
        );
    }

    public String getMaintenanceKickMessage() {
        return this.config.getString(
                "reset.maintenance-kick-message",
                "§c[CPVPSingleBiome] §fArenas are resetting. Please reconnect shortly."
        );
    }

    public boolean isWarningEnabled() {
        return this.config.getBoolean(
                "reset.warning-enabled",
                true
        );
    }

    public List<Integer> getWarningMinutes() {
        List<Integer> raw =
                this.config.getIntegerList("reset.warning-minutes");

        if (raw == null || raw.isEmpty()) {
            return List.of(10, 5, 1);
        }

        return raw;
    }

    public boolean isBackupOldWorlds() {
        return this.config.getBoolean(
                "reset.backup-old-worlds",
                true
        );
    }

    public boolean isDeleteOldWorldsAfterSuccess() {
        return this.config.getBoolean(
                "reset.delete-old-worlds-after-success",
                false
        );
    }

    public boolean isMaintenanceDuringReset() {
        return this.config.getBoolean(
                "reset.maintenance-during-reset",
                false
        );
    }

    public String getMaintenanceCommandOn() {
        return this.config.getString(
                "reset.maintenance-command-on",
                "main on"
        );
    }

    public String getMaintenanceCommandOff() {
        return this.config.getString(
                "reset.maintenance-command-off",
                "main off"
        );
    }

    public int getResetIntervalDays() {
        int v = this.config.getInt("reset.interval-days", 1);

        return Math.max(1, v);
    }

    public LocalDate getLastResetDate() {
        String raw = this.config.getString(
                "reset.last-reset-date",
                ""
        );

        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(raw.trim());
        } catch (Exception e) {
            this.plugin.getLogger().warning(
                    "Invalid reset.last-reset-date in config.yml: '"
                            + raw
                            + "'. Expected format: YYYY-MM-DD"
            );

            return null;
        }
    }

    public void setLastResetDate(LocalDate date) {
        String value =
                date == null
                        ? ""
                        : date.toString();

        this.config.set(
                "reset.last-reset-date",
                value
        );

        writeLastResetDateToConfigFile(value);

        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();
    }

    private void writeLastResetDateToConfigFile(String value) {
        Path configPath =
                this.plugin.getDataFolder()
                        .toPath()
                        .resolve("config.yml");

        if (!Files.exists(configPath)) {
            this.plugin.getLogger().warning(
                    "Could not update reset.last-reset-date because config.yml does not exist yet. Falling back to saveConfig()."
            );

            this.plugin.saveConfig();
            return;
        }

        List<String> lines;

        try {
            lines =
                    Files.readAllLines(
                            configPath,
                            StandardCharsets.UTF_8
                    );

        } catch (IOException e) {
            this.plugin.getLogger().warning(
                    "Could not read config.yml to update reset.last-reset-date: "
                            + e.getMessage()
                            + ". Falling back to saveConfig()."
            );

            this.plugin.saveConfig();
            return;
        }

        List<String> out =
                new ArrayList<>();

        boolean inResetSection =
                false;

        boolean wroteLastResetDate =
                false;

        boolean insertedAfterIntervalDays =
                false;

        for (String line : lines) {
            String trimmed =
                    line.trim();

            if (isTopLevelYamlSection(line, trimmed)) {
                if (inResetSection && !wroteLastResetDate && !insertedAfterIntervalDays) {
                    out.add(
                            "  last-reset-date: "
                                    + quoteYamlString(value)
                                    + "                    # YYYY-MM-DD | auto-written after full reset; empty = no full reset recorded yet"
                    );

                    insertedAfterIntervalDays = true;
                    wroteLastResetDate = true;
                }

                inResetSection =
                        trimmed.equals("reset:");
            }

            if (inResetSection
                    && trimmed.startsWith("last-reset-date:")) {
                if (!wroteLastResetDate) {
                    out.add(
                            rebuildLastResetDateLine(
                                    line,
                                    value
                            )
                    );

                    wroteLastResetDate = true;
                }

                continue;
            }

            out.add(line);
        }

        if (inResetSection && !wroteLastResetDate && !insertedAfterIntervalDays) {
            out.add(
                    "  last-reset-date: "
                            + quoteYamlString(value)
                            + "                    # YYYY-MM-DD | auto-written after full reset; empty = no full reset recorded yet"
            );

            wroteLastResetDate = true;
        }

        if (!wroteLastResetDate) {
            this.plugin.getLogger().warning(
                    "Could not find reset section in config.yml. Falling back to saveConfig()."
            );

            this.plugin.saveConfig();
            return;
        }

        try {
            Files.write(
                    configPath,
                    out,
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {
            this.plugin.getLogger().warning(
                    "Could not write reset.last-reset-date to config.yml: "
                            + e.getMessage()
                            + ". Falling back to saveConfig()."
            );

            this.plugin.saveConfig();
        }
    }

    private boolean isTopLevelYamlSection(
            String line,
            String trimmed
    ) {
        if (trimmed.isEmpty()) {
            return false;
        }

        if (trimmed.startsWith("#")) {
            return false;
        }

        if (line.startsWith(" ")
                || line.startsWith("\t")) {
            return false;
        }

        return trimmed.endsWith(":");
    }

    private String rebuildLastResetDateLine(
            String originalLine,
            String value
    ) {
        int keyIndex =
                originalLine.indexOf("last-reset-date:");

        String indent =
                keyIndex > 0
                        ? originalLine.substring(0, keyIndex)
                        : "";

        int commentIndex =
                originalLine.indexOf('#');

        String comment =
                commentIndex >= 0
                        ? originalLine.substring(commentIndex)
                        : "";

        if (comment.isBlank()) {
            return indent
                    + "last-reset-date: "
                    + quoteYamlString(value);
        }

        return indent
                + "last-reset-date: "
                + quoteYamlString(value)
                + "                    "
                + comment;
    }

    private String quoteYamlString(String value) {
        String safe =
                value == null
                        ? ""
                        : value.replace("\"", "\\\"");

        return "\""
                + safe
                + "\"";
    }

    public Difficulty getResetDifficulty() {
        String raw = this.config.getString(
                "reset.world-settings.difficulty",
                "HARD"
        );

        return parseDifficulty(raw);
    }

    public GameMode getResetGameMode() {
        String raw = this.config.getString(
                "reset.world-settings.gamemode",
                "SURVIVAL"
        );

        return parseGameMode(raw);
    }

    public boolean isResetPvpEnabled() {
        return this.config.getBoolean(
                "reset.world-settings.pvp",
                true
        );
    }

    private Difficulty parseDifficulty(String raw) {
        if (raw == null || raw.isBlank()) {
            return Difficulty.HARD;
        }

        String value = raw.trim().toUpperCase(Locale.ROOT);

        return switch (value) {
            case "0", "PEACEFUL" -> Difficulty.PEACEFUL;
            case "1", "EASY" -> Difficulty.EASY;
            case "2", "NORMAL" -> Difficulty.NORMAL;
            case "3", "HARD" -> Difficulty.HARD;
            default -> Difficulty.HARD;
        };
    }

    private GameMode parseGameMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return GameMode.SURVIVAL;
        }

        String value = raw.trim().toUpperCase(Locale.ROOT);

        return switch (value) {
            case "0", "SURVIVAL" -> GameMode.SURVIVAL;
            case "1", "CREATIVE" -> GameMode.CREATIVE;
            case "2", "ADVENTURE" -> GameMode.ADVENTURE;
            case "3", "SPECTATOR" -> GameMode.SPECTATOR;
            default -> GameMode.SURVIVAL;
        };
    }

    public List<ResetWorldSpec> getResetWorlds() {
        List<ResetWorldSpec> out = new ArrayList<>();

        List<Map<?, ?>> raw =
                this.config.getMapList("reset.worlds");

        for (Map<?, ?> entry : raw) {
            Object w = entry.get("world");

            if (w == null) {
                continue;
            }

            Object b = entry.get("biome");

            String biome =
                    (b != null)
                            ? b.toString()
                            : getDefaultBiome();

            out.add(
                    new ResetWorldSpec(
                            w.toString(),
                            biome
                    )
            );
        }

        return out;
    }

    public boolean isChunkyEnabled() {
        return this.config.getBoolean("chunky.enabled", false);
    }

    public int getChunkyRadius() {
        int v = this.config.getInt("chunky.radius", 1000);

        return Math.max(0, v);
    }

    public String getChunkyShape() {
        return this.config.getString("chunky.shape", "circle");
    }

    public boolean isChunkyCenterSpawn() {
        return this.config.getBoolean("chunky.center-spawn", true);
    }

    public boolean isChunkySilent() {
        return this.config.getBoolean("chunky.silent", false);
    }

    public int getChunkyDelaySeconds() {
        int v = this.config.getInt(
                "chunky.delay-after-world-create-seconds",
                10
        );

        return Math.max(0, v);
    }
}
