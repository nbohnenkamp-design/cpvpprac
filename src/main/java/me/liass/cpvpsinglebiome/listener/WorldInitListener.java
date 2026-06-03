package me.liass.cpvpsinglebiome.listener;

import me.liass.cpvpsinglebiome.generator.SingleBiomeChunkGenerator;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

public class WorldInitListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldInit(WorldInitEvent event) {
        World world = event.getWorld();

        if (!(world.getGenerator() instanceof SingleBiomeChunkGenerator)) {
            return;
        }

        applyArenaRules(world);
    }

    public static void applyArenaRules(World world) {
        if (world == null) {
            return;
        }

        world.setSpawnFlags(false, false);

        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);

        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);

        world.setStorm(false);
        world.setThundering(false);
    }
}
