package eu.cpvpprac.practicebot;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Husk;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/**
 * Equips a bot entity with armour, weapon, shield, and potion effects.
 *
 * Original methods (applyArmor, applyShield) are unchanged.
 * applyKit() is a new addition that drives the kit preset system.
 */
public class ArmorUtil {

    // -------------------------------------------------------------------------
    // Original methods — unchanged
    // -------------------------------------------------------------------------

    public static void applyArmor(Husk entity, ConfigManager cfg) {
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return;

        // Zero all drop chances — nothing falls on death
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
        if (ench != null) meta.addEnchant(ench, 4, true);

        item.setItemMeta(meta);
        return item;
    }

    public static void applyShield(Husk entity) {
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return;
        eq.setItemInOffHand(new ItemStack(Material.SHIELD));
        eq.setItemInOffHandDropChance(0f);
    }

    // -------------------------------------------------------------------------
    // Kit preset application — new
    // -------------------------------------------------------------------------

    /**
     * Applies a full {@link KitPreset} to a Husk entity:
     * <ul>
     *   <li>Armor (material + enchantment on all four pieces)</li>
     *   <li>Weapon in main hand (with Sharpness if configured)</li>
     *   <li>Shield or empty off-hand</li>
     *   <li>Persistent potion effects (existing effects are cleared first)</li>
     * </ul>
     * Drop chances are zeroed for every slot so nothing falls on death.
     */
    public static void applyKit(Husk entity, KitPreset kit) {
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) return;

        // Zero all drop chances
        eq.setHelmetDropChance(0f);
        eq.setChestplateDropChance(0f);
        eq.setLeggingsDropChance(0f);
        eq.setBootsDropChance(0f);
        eq.setItemInMainHandDropChance(0f);
        eq.setItemInOffHandDropChance(0f);

        // Armor
        applyArmorFromKit(eq, kit.getArmorMaterial(), kit.getArmorEnchant());

        // Weapon
        String weaponMat = kit.getWeaponMaterial();
        if (!"NONE".equals(weaponMat)) {
            try {
                Material mat    = Material.valueOf(weaponMat);
                ItemStack weapon = new ItemStack(mat);
                if (kit.getSharpnessLevel() > 0) {
                    ItemMeta meta = weapon.getItemMeta();
                    if (meta != null) {
                        meta.addEnchant(Enchantment.SHARPNESS, kit.getSharpnessLevel(), true);
                        weapon.setItemMeta(meta);
                    }
                }
                eq.setItemInMainHand(weapon);
            } catch (IllegalArgumentException e) {
                // Invalid material name in config — leave main hand empty
                eq.setItemInMainHand(new ItemStack(Material.AIR));
            }
        } else {
            eq.setItemInMainHand(new ItemStack(Material.AIR));
        }

        // Shield / off-hand
        if (kit.isShield()) {
            eq.setItemInOffHand(new ItemStack(Material.SHIELD));
        } else {
            eq.setItemInOffHand(new ItemStack(Material.AIR));
        }

        // Potion effects — clear all active effects, then apply kit effects
        for (PotionEffect active : entity.getActivePotionEffects()) {
            entity.removePotionEffect(active.getType());
        }
        for (Map.Entry<PotionEffectType, Integer> entry : kit.getEffects().entrySet()) {
            entity.addPotionEffect(
                    new PotionEffect(entry.getKey(), Integer.MAX_VALUE, entry.getValue(), false, false, false));
        }
    }

    /** Resolves armor pieces for a given material string and applies the enchantment. */
    private static void applyArmorFromKit(EntityEquipment eq, String armorMaterial, String armorEnchant) {
        if ("NONE".equals(armorMaterial)) {
            eq.setHelmet(new ItemStack(Material.AIR));
            eq.setChestplate(new ItemStack(Material.AIR));
            eq.setLeggings(new ItemStack(Material.AIR));
            eq.setBoots(new ItemStack(Material.AIR));
            return;
        }

        Material helmetMat, chestMat, legsMat, bootsMat;
        if ("NETHERITE".equals(armorMaterial)) {
            helmetMat = Material.NETHERITE_HELMET;
            chestMat  = Material.NETHERITE_CHESTPLATE;
            legsMat   = Material.NETHERITE_LEGGINGS;
            bootsMat  = Material.NETHERITE_BOOTS;
        } else {
            // Default to DIAMOND for any unrecognised value
            helmetMat = Material.DIAMOND_HELMET;
            chestMat  = Material.DIAMOND_CHESTPLATE;
            legsMat   = Material.DIAMOND_LEGGINGS;
            bootsMat  = Material.DIAMOND_BOOTS;
        }

        eq.setHelmet(enchant(new ItemStack(helmetMat), armorEnchant));
        eq.setChestplate(enchant(new ItemStack(chestMat), armorEnchant));
        eq.setLeggings(enchant(new ItemStack(legsMat), armorEnchant));
        eq.setBoots(enchant(new ItemStack(bootsMat), armorEnchant));
    }
}
