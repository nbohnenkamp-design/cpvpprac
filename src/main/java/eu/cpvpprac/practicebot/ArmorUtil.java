package eu.cpvpprac.practicebot;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Husk;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Equips a bot entity with armour and optional shield based on config values.
 */
public class ArmorUtil {

    public static void applyArmor(Husk entity, ConfigManager cfg) {
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return;

        // Zero out all drop chances so nothing falls on death
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
        eq.setItemInMainHandDropChance(0f);
        eq.setItemInOffHandDropChance(0f);

        if ("NONE".equals(cfg.armorType)) return;

        Material helmetMat, chestMat, legsMat, bootsMat;
        if ("NETHERITE".equals(cfg.armorType)) {
            helmetMat = Material.NETHERITE_HELMET;
            chestMat  = Material.NETHERITE_CHESTPLATE;
            legsMat   = Material.NETHERITE_LEGGINGS;
            bootsMat  = Material.NETHERITE_BOOTS;
        } else {
            // Default: DIAMOND
            helmetMat = Material.DIAMOND_HELMET;
            chestMat  = Material.DIAMOND_CHESTPLATE;
            legsMat   = Material.DIAMOND_LEGGINGS;
            bootsMat  = Material.DIAMOND_BOOTS;
        }

        eq.setHelmet(enchant(new ItemStack(helmetMat), cfg.armorEnchantment));
        eq.setChestplate(enchant(new ItemStack(chestMat), cfg.armorEnchantment));
        eq.setLeggings(enchant(new ItemStack(legsMat), cfg.armorEnchantment));
        eq.setBoots(enchant(new ItemStack(bootsMat), cfg.armorEnchantment));
    }

    private static ItemStack enchant(ItemStack item, String enchantType) {
        if ("NONE".equals(enchantType)) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        Enchantment ench = switch (enchantType) {
            case "PROTECTION_4"       -> Enchantment.PROTECTION;
            case "BLAST_PROTECTION_4" -> Enchantment.BLAST_PROTECTION;
            default                   -> null;
        };

        if (ench != null) {
            // true = bypass level restriction
            meta.addEnchant(ench, 4, true);
        }

        item.setItemMeta(meta);
        return item;
    }

    public static void applyShield(Husk entity) {
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return;
        eq.setItemInOffHand(new ItemStack(Material.SHIELD));
        eq.setItemInOffHandDropChance(0f);
    }
}
