/*    */ package me.liass.cpvpsinglebiome.generator;
/*    */ 
/*    */ import java.util.Collections;
/*    */ import java.util.List;
/*    */ import org.bukkit.block.Biome;
/*    */ import org.bukkit.generator.BiomeProvider;
/*    */ import org.bukkit.generator.WorldInfo;
/*    */ 
/*    */ public class SingleBiomeProvider
/*    */   extends BiomeProvider
/*    */ {
/*    */   private final Biome biome;
/*    */   private final List<Biome> biomes;
/*    */   
/*    */   public SingleBiomeProvider(Biome biome) {
/* 16 */     this.biome = biome;
/* 17 */     this.biomes = Collections.singletonList(biome);
/*    */   }
/*    */ 
/*    */   
/*    */   public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
/* 22 */     return this.biome;
/*    */   }
/*    */ 
/*    */   
/*    */   public List<Biome> getBiomes(WorldInfo worldInfo) {
/* 27 */     return this.biomes;
/*    */   }
/*    */ }


/* Location:              /home/norbert/IdeaProjects/cpvpprac/target/CPVPSingleBiome-1.0.0.jar!/me/liass/cpvpsinglebiome/generator/SingleBiomeProvider.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */