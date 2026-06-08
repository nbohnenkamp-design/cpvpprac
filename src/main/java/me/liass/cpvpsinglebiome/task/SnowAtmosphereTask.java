package me.liass.cpvpsinglebiome.task;

import java.util.Locale;
import java.util.Random;

import me.liass.cpvpsinglebiome.config.ConfigManager;
import me.liass.cpvpsinglebiome.reset.ResetWorldSpec;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class SnowAtmosphereTask implements Runnable {

    private final ConfigManager config;
    private final Random random = new Random();

    public SnowAtmosphereTask(ConfigManager config) {
        this.config = config;
    }

    @Override
    public void run() {
        if (!this.config.isSnowflakeParticlesEnabled()) {
            return;
        }

        int particlesPerPlayer =
                this.config.getSnowflakeParticlesPerPlayer();

        if (particlesPerPlayer <= 0) {
            return;
        }

        double radius =
                this.config.getSnowflakeParticleRadius();

        double height =
                this.config.getSnowflakeParticleHeight();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }

            World world =
                    player.getWorld();

            if (!isSnowWorld(world)) {
                continue;
            }

            spawnSnowflakesAroundPlayer(
                    player,
                    particlesPerPlayer,
                    radius,
                    height
            );
        }
    }

    private boolean isSnowWorld(World world) {
        if (world == null) {
            return false;
        }

        String worldName =
                world.getName();

        if (worldName == null || worldName.isBlank()) {
            return false;
        }

        /*
         * Primary check:
         * Use reset.worlds from config.yml, because those are the controlled
         * CPVPSingleBiome arena worlds.
         */
        for (ResetWorldSpec spec : this.config.getResetWorlds()) {
            if (spec == null) {
                continue;
            }

            if (!"snow".equalsIgnoreCase(spec.biomeName())) {
                continue;
            }

            if (worldName.equalsIgnoreCase(spec.worldName())) {
                return true;
            }
        }

        /*
         * Fallback:
         * The normal production snow world is simply named "snow".
         * This also keeps the effect working even if reset.worlds is incomplete.
         */
        return worldName.toLowerCase(Locale.ROOT).equals("snow");
    }

    private void spawnSnowflakesAroundPlayer(
            Player player,
            int particlesPerPlayer,
            double radius,
            double height
    ) {
        Location base =
                player.getLocation();

        World world =
                base.getWorld();

        if (world == null) {
            return;
        }

        for (int i = 0; i < particlesPerPlayer; i++) {
            double offsetX =
                    randomOffset(radius);

            double offsetZ =
                    randomOffset(radius);

            double offsetY =
                    1.5D + this.random.nextDouble() * height;

            Location particleLocation =
                    base.clone().add(
                            offsetX,
                            offsetY,
                            offsetZ
                    );

            world.spawnParticle(
                    Particle.SNOWFLAKE,
                    particleLocation,
                    1,
                    0.02D,
                    0.02D,
                    0.02D,
                    0.0D
            );
        }
    }

    private double randomOffset(double radius) {
        return (this.random.nextDouble() * 2.0D - 1.0D)
                * radius;
    }
}
