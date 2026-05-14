// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 Norbert Bohnenkamp
package com.cpvpprac.singlebio;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConfigGui {

    static final int SIZE = 54;
    static final Component TITLE = Component.text("CPVPSingleBiome Config", NamedTextColor.DARK_PURPLE)
            .decorate(TextDecoration.BOLD);

    // Row 0: world toggles (slots 0–5), glass filler 6–8
    static final int WORLD_SLOT_START = 0;
    static final int WORLD_SLOT_MAX   = 6; // up to 6 worlds

    // Row 3: action controls
    static final int SLOT_RESET_TOGGLE  = 27;
    static final int SLOT_RESET_NOW     = 28;
    static final int SLOT_CHUNKY_TOGGLE = 29;
    static final int SLOT_CHUNKY_ALL    = 30;

    // Row 4
    static final int SLOT_MAINT_TOGGLE = 36;

    // Row 5
    static final int SLOT_RELOAD = 52;
    static final int SLOT_CLOSE  = 53;

    private static final Map<UUID, Inventory> OPEN = new HashMap<>();

    public static void open(Player player, CPVPSingleBiome plugin) {
        Inventory inv = build(plugin);
        OPEN.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    static void refresh(Player player, CPVPSingleBiome plugin) {
        Inventory inv = OPEN.get(player.getUniqueId());
        if (inv == null) return;
        Inventory fresh = build(plugin);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, fresh.getItem(i));
    }

    static boolean isOpen(Player player, Inventory inv) {
        return inv.equals(OPEN.get(player.getUniqueId()));
    }

    static void close(Player player) {
        OPEN.remove(player.getUniqueId());
    }

    static List<String> getWorldKeys(CPVPSingleBiome plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("worlds");
        if (section == null) return List.of();
        return new ArrayList<>(section.getKeys(false));
    }

    // -------------------------------------------------------------------------

    private static Inventory build(CPVPSingleBiome plugin) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        ConfigManager cfg = plugin.getConfigManager();
        ItemStack glass = glass();

        for (int i = 0; i < SIZE; i++) inv.setItem(i, glass);

        // World toggles
        List<String> keys = getWorldKeys(plugin);
        for (int i = 0; i < Math.min(keys.size(), WORLD_SLOT_MAX); i++) {
            String key = keys.get(i);
            boolean enabled = cfg.isWorldEnabled(key);
            inv.setItem(WORLD_SLOT_START + i, worldItem(key, cfg.getBiomeName(key), enabled));
        }

        // Reset controls
        inv.setItem(SLOT_RESET_TOGGLE, toggleItem(
                "Auto Reset", "Scheduled daily arena reset",
                cfg.isResetEnabled(), Material.CLOCK));
        inv.setItem(SLOT_RESET_NOW, actionItem(
                "Reset Now", "Immediately reset all enabled worlds", Material.TNT));
        inv.setItem(SLOT_CHUNKY_TOGGLE, toggleItem(
                "Chunky Pre-gen", "Run Chunky after each reset",
                cfg.isChunkyEnabled(), Material.GRASS_BLOCK));
        inv.setItem(SLOT_CHUNKY_ALL, actionItem(
                "Start Chunky (All)", "Trigger Chunky for all enabled worlds now",
                Material.MAP));

        // Maintenance
        inv.setItem(SLOT_MAINT_TOGGLE, toggleItem(
                "Maintenance Mode", "Kick players during resets",
                cfg.isMaintenanceEnabled(), Material.ORANGE_STAINED_GLASS_PANE));

        // Bottom actions
        inv.setItem(SLOT_RELOAD, actionItem(
                "Reload Config", "Reload config.yml and restart scheduler",
                Material.COMPARATOR));
        inv.setItem(SLOT_CLOSE, actionItem(
                "Close", "Close this menu", Material.BARRIER));

        return inv;
    }

    // -------------------------------------------------------------------------

    private static ItemStack worldItem(String key, String biome, boolean enabled) {
        Material mat = enabled ? Material.GRASS_BLOCK : Material.DIRT;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(capitalize(key), enabled ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false)
                .decorate(TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(loreText("Biome: ").append(Component.text(biome, NamedTextColor.YELLOW)));
        lore.add(loreText("Status: ").append(Component.text(
                enabled ? "Enabled" : "Disabled",
                enabled ? NamedTextColor.GREEN : NamedTextColor.RED)));
        lore.add(Component.empty());
        lore.add(hint("Left-click to toggle"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack toggleItem(String name, String desc, boolean state, Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
                .decorate(TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(loreText(desc));
        lore.add(Component.empty());
        lore.add(loreText("Status: ").append(Component.text(
                state ? "ON" : "OFF", state ? NamedTextColor.GREEN : NamedTextColor.RED)));
        lore.add(hint("Left-click to toggle"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack actionItem(String name, String desc, Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false)
                .decorate(TextDecoration.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(loreText(desc));
        lore.add(Component.empty());
        lore.add(hint("Left-click to execute"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack glass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static Component loreText(String text) {
        return Component.text(text, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private static Component hint(String text) {
        return Component.text(text, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
