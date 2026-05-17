package me.liass.cpvpsinglebiome.generator;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Biome;

public enum BiomeType {

    PLAINS(
            "plains",
            Material.GRASS_BLOCK,
            Material.DIRT,
            Biome.PLAINS
    ),

    DESERT(
            "desert",
            Material.SAND,
            Material.SANDSTONE,
            Biome.DESERT
    ),

    BADLANDS(
            "badlands",
            Material.RED_SAND,
            Material.TERRACOTTA,
            Biome.BADLANDS
    ),

    SNOW(
            "snow",
            Material.GRASS_BLOCK,
            Material.DIRT,
            Biome.SNOWY_PLAINS
    ),

    MUSHROOM(
            "mushroom",
            Material.MYCELIUM,
            Material.DIRT,
            Biome.MUSHROOM_FIELDS
    ),

    END(
            "end",
            Material.END_STONE,
            Material.END_STONE,
            Biome.THE_END
    );

    private final String id;
    private final Material surfaceMaterial;
    private final Material subSurfaceMaterial;
    private final Biome paperBiome;

    BiomeType(
            String id,
            Material surfaceMaterial,
            Material subSurfaceMaterial,
            Biome paperBiome
    ) {
        this.id = id;
        this.surfaceMaterial = surfaceMaterial;
        this.subSurfaceMaterial = subSurfaceMaterial;
        this.paperBiome = paperBiome;
    }

    public String getId() {
        return id;
    }

    public Material getSurfaceMaterial() {
        return surfaceMaterial;
    }

    public Material getSubSurfaceMaterial() {
        return subSurfaceMaterial;
    }

    public Biome getPaperBiome() {
        return paperBiome;
    }

    public static BiomeType fromString(String input) {
        if (input == null) {
            return null;
        }

        String normalized = input.trim().toLowerCase();

        for (BiomeType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }

        return null;
    }

    public static BiomeType fromStringOrDefault(
            String input,
            BiomeType fallback
    ) {
        BiomeType parsed = fromString(input);

        return parsed != null ? parsed : fallback;
    }

    public static List<String> getNames() {
        List<String> names = new ArrayList<>();

        for (BiomeType type : values()) {
            names.add(type.id);
        }

        return names;
    }
}
