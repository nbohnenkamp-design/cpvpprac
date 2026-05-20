package me.liass.cpvpsinglebiome.listener;

import me.liass.cpvpsinglebiome.CPVPSingleBiomePlugin;
import me.liass.cpvpsinglebiome.reset.ResetManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class MaintenanceJoinListener implements Listener {

    private final CPVPSingleBiomePlugin plugin;

    public MaintenanceJoinListener(CPVPSingleBiomePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        ResetManager resetManager = plugin.getResetManager();

        if (resetManager == null) {
            return;
        }

        if (!resetManager.isMaintenanceActive()) {
            return;
        }

        if (resetManager.canBypassMaintenance(event.getPlayer())) {
            return;
        }

        event.disallow(
                PlayerLoginEvent.Result.KICK_OTHER,
                plugin.getConfigManager().getMaintenanceKickMessage()
        );
    }
}
