// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 Norbert Bohnenkamp
package com.cpvpprac.singlebio;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.List;

public class ConfigGuiListener implements Listener {

    private final CPVPSingleBiome plugin;

    public ConfigGuiListener(CPVPSingleBiome plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!ConfigGui.isOpen(player, event.getInventory())) return;

        event.setCancelled(true);

        // Ignore clicks on the player's own inventory or outside any inventory
        if (event.getClickedInventory() == null) return;
        if (!event.getInventory().equals(event.getClickedInventory())) return;

        int slot = event.getSlot();
        ConfigManager cfg = plugin.getConfigManager();

        // World toggle slots (0–5)
        if (slot >= ConfigGui.WORLD_SLOT_START && slot < ConfigGui.WORLD_SLOT_START + ConfigGui.WORLD_SLOT_MAX) {
            List<String> keys = ConfigGui.getWorldKeys(plugin);
            int index = slot - ConfigGui.WORLD_SLOT_START;
            if (index >= keys.size()) return;
            String worldKey = keys.get(index);
            cfg.setWorldEnabled(worldKey, !cfg.isWorldEnabled(worldKey));
            plugin.getServer().getScheduler().runTask(plugin, () -> ConfigGui.refresh(player, plugin));
            return;
        }

        switch (slot) {
            case ConfigGui.SLOT_RESET_TOGGLE -> {
                boolean next = !cfg.isResetEnabled();
                cfg.setResetEnabled(next);
                plugin.getResetManager().shutdown();
                if (next) plugin.getResetManager().startScheduler();
                plugin.getServer().getScheduler().runTask(plugin, () -> ConfigGui.refresh(player, plugin));
            }
            case ConfigGui.SLOT_RESET_NOW -> {
                player.closeInventory();
                player.sendMessage("§eStarting full arena reset…");
                plugin.getServer().getScheduler().runTask(plugin, plugin.getResetManager()::resetAllWorlds);
            }
            case ConfigGui.SLOT_CHUNKY_TOGGLE -> {
                cfg.setChunkyEnabled(!cfg.isChunkyEnabled());
                plugin.getServer().getScheduler().runTask(plugin, () -> ConfigGui.refresh(player, plugin));
            }
            case ConfigGui.SLOT_CHUNKY_ALL -> {
                player.closeInventory();
                List<String> worlds = cfg.getEnabledWorlds();
                if (worlds.isEmpty()) {
                    player.sendMessage("§cNo enabled worlds in config.");
                    return;
                }
                for (String world : worlds) plugin.getResetManager().triggerChunky(world);
                player.sendMessage("§aChunky pregeneration started for: §f" + String.join(", ", worlds));
            }
            case ConfigGui.SLOT_MAINT_TOGGLE -> {
                cfg.setMaintenanceEnabled(!cfg.isMaintenanceEnabled());
                plugin.getServer().getScheduler().runTask(plugin, () -> ConfigGui.refresh(player, plugin));
            }
            case ConfigGui.SLOT_RELOAD -> {
                cfg.reload();
                plugin.getResetManager().shutdown();
                plugin.getResetManager().startScheduler();
                player.sendMessage("§aConfig reloaded and scheduler restarted.");
                plugin.getServer().getScheduler().runTask(plugin, () -> ConfigGui.refresh(player, plugin));
            }
            case ConfigGui.SLOT_CLOSE -> player.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        ConfigGui.close(player);
    }
}
