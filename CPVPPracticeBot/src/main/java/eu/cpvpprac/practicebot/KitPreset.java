package eu.cpvpprac.practicebot;

import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable snapshot of a named equipment kit loaded from config.yml.
 *
 * All string fields (armorMaterial, armorEnchant, weaponMaterial) are stored
 * in UPPER_CASE exactly as they appear in the config so they can be passed
 * directly to Material.valueOf() and compared with simple equality checks.
 *
 * Potion effects are stored as a map of PotionEffectType → amplifier (0-indexed,
 * so 0 = level I, 1 = level II, etc.), matching Bukkit's PotionEffect convention.
 */
public class KitPreset {

    private final String name;

    /** NONE | DIAMOND | NETHERITE */
    private final String armorMaterial;

    /** NONE | PROTECTION_4 | BLAST_PROTECTION_4 */
    private final String armorEnchant;

    /** NONE | DIAMOND_SWORD | NETHERITE_SWORD | DIAMOND_AXE | NETHERITE_AXE */
    private final String weaponMaterial;

    /** Sharpness level to apply to the weapon (0 = no enchant). */
    private final int sharpnessLevel;

    /** Whether to put a shield in the bot's off-hand. */
    private final boolean shield;

    /** Number of totems tracked for this kit (used by future AutoTotem logic). */
    private final int totems;

    /**
     * Persistent potion effects to apply when this kit is equipped.
     * Key = PotionEffectType, Value = amplifier (0-indexed).
     */
    private final Map<PotionEffectType, Integer> effects;

    public KitPreset(
            String name,
            String armorMaterial,
            String armorEnchant,
            String weaponMaterial,
            int sharpnessLevel,
            boolean shield,
            int totems,
            Map<PotionEffectType, Integer> effects) {

        this.name           = name;
        this.armorMaterial  = armorMaterial;
        this.armorEnchant   = armorEnchant;
        this.weaponMaterial = weaponMaterial;
        this.sharpnessLevel = sharpnessLevel;
        this.shield         = shield;
        this.totems         = totems;
        this.effects        = Collections.unmodifiableMap(effects);
    }

    public String getName()           { return name; }
    public String getArmorMaterial()  { return armorMaterial; }
    public String getArmorEnchant()   { return armorEnchant; }
    public String getWeaponMaterial() { return weaponMaterial; }
    public int    getSharpnessLevel() { return sharpnessLevel; }
    public boolean isShield()         { return shield; }
    public int    getTotems()         { return totems; }

    /** Returns an unmodifiable view of the effects map. */
    public Map<PotionEffectType, Integer> getEffects() { return effects; }
}
