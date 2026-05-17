/*     */ package me.liass.cpvpsinglebiome.generator;
/*     */ 
/*     */ import java.util.Collections;
/*     */ import java.util.List;
/*     */ import java.util.Random;
/*     */ import me.liass.cpvpsinglebiome.config.ConfigManager;
/*     */ import me.liass.cpvpsinglebiome.populator.CPVPSBBlockPopulator;
/*     */ import org.bukkit.Material;
/*     */ import org.bukkit.World;
/*     */ import org.bukkit.generator.BiomeProvider;
/*     */ import org.bukkit.generator.BlockPopulator;
/*     */ import org.bukkit.generator.ChunkGenerator;
/*     */ import org.bukkit.generator.WorldInfo;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class SingleBiomeChunkGenerator
/*     */   extends ChunkGenerator
/*     */ {
/*     */   private static final int FBM_OCTAVES = 4;
/*     */   private static final double FBM_PERSISTENCE = 0.5D;
/*     */   private static final int BEDROCK_LAYERS = 2;
/*     */   private static final int SUB_DEPTH = 4;
/*     */   private final ConfigManager config;
/*     */   private final BiomeType biomeType;
/*     */   private volatile SimpleNoise noise;
/*     */   private volatile long cachedSeed;
/*     */   
/*     */   public SingleBiomeChunkGenerator(ConfigManager config, BiomeType biomeType) {
/*  47 */     this.config = config;
/*  48 */     this.biomeType = (biomeType != null) ? biomeType : BiomeType.DESERT;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean shouldGenerateNoise() {
/*  55 */     return false;
/*  56 */   } public boolean shouldGenerateSurface() { return false; }
/*  57 */   public boolean shouldGenerateBedrock() { return false; }
/*  58 */   public boolean shouldGenerateCaves() { return false; }
/*  59 */   public boolean shouldGenerateDecorations() { return false; }
/*  60 */   public boolean shouldGenerateMobs() { return false; } public boolean shouldGenerateStructures() {
/*  61 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData) {
/*  77 */     placeChunk(worldInfo, chunkX, chunkZ, chunkData);
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData) {
/*  83 */     placeChunk(worldInfo, chunkX, chunkZ, chunkData);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void placeChunk(WorldInfo worldInfo, int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData) {
/*  91 */     SimpleNoise n = getOrCreateNoise(worldInfo.getSeed());
/*     */     
/*  93 */     int baseHeight = this.config.getBaseHeight();
/*  94 */     double noiseScale = this.config.getNoiseScale();
/*     */     
/*  96 */     double variation = this.config.getHeightVariation() * (1.0D - clamp01(this.config.getFlatness()));
/*     */     
/*  98 */     int minH = worldInfo.getMinHeight();
/*  99 */     int maxH = worldInfo.getMaxHeight() - 1;
/*     */     
/* 101 */     Material fill = (this.biomeType == BiomeType.END) ? Material.END_STONE : Material.STONE;
/* 102 */     Material subMat = this.biomeType.getSubSurfaceMaterial();
/* 103 */     Material surfMat = this.biomeType.getSurfaceMaterial();
/*     */ 
/*     */     
/* 106 */     int minSurf = minH + 2 + 4 + 1;
/* 107 */     int maxSurf = maxH - 1;
/*     */     
/* 109 */     for (int x = 0; x < 16; x++) {
/* 110 */       for (int z = 0; z < 16; z++) {
/* 111 */         int wx = chunkX * 16 + x;
/* 112 */         int wz = chunkZ * 16 + z;
/*     */ 
/*     */ 
/*     */ 
/*     */         
/* 117 */         double nv = n.octaveNoise(wx / noiseScale, wz / noiseScale, 4, 0.5D);
/*     */ 
/*     */         
/* 120 */         int surfY = (int)Math.round(baseHeight + nv * variation);
/* 121 */         if (surfY < minSurf) surfY = minSurf; 
/* 122 */         if (surfY > maxSurf) surfY = maxSurf;
/*     */ 
/*     */         
/* 125 */         for (int y = minH; y < minH + 2; y++) {
/* 126 */           chunkData.setBlock(x, y, z, Material.BEDROCK);
/*     */         }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */         
/* 133 */         int fillTop = surfY - 4 - 1; int i;
/* 134 */         for (i = minH + 2; i <= fillTop; i++) {
/*     */           
/* 136 */           Material fillBlock = (this.biomeType != BiomeType.END && i < 0) ? Material.DEEPSLATE : fill;
/* 137 */           chunkData.setBlock(x, i, z, fillBlock);
/*     */         } 
/*     */ 
/*     */ 
/*     */         
/* 142 */         for (i = surfY - 4; i < surfY; i++) {
/* 143 */           chunkData.setBlock(x, i, z, subMat);
/*     */         }
/*     */ 
/*     */         
/* 147 */         chunkData.setBlock(x, surfY, z, surfMat);
/*     */ 
/*     */         
/* 150 */         if (this.biomeType == BiomeType.SNOW && surfY + 1 <= maxH) {
/* 151 */           chunkData.setBlock(x, surfY + 1, z, Material.SNOW);
/*     */         }
/*     */       } 
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
/* 161 */     return new SingleBiomeProvider(this.biomeType.getPaperBiome());
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public List<BlockPopulator> getDefaultPopulators(World world) {
/* 174 */     if (this.config == null || !this.config.isDecorationEnabled() || this.biomeType == BiomeType.END) {
/* 175 */       return Collections.emptyList();
/*     */     }
/* 177 */     return (List)Collections.singletonList(new CPVPSBBlockPopulator(this.config, this.biomeType));
/*     */   }
/*     */   public BiomeType getBiomeType() {
/* 180 */     return this.biomeType;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private SimpleNoise getOrCreateNoise(long seed) {
/* 187 */     SimpleNoise local = this.noise;
/* 188 */     if (local != null && this.cachedSeed == seed) return local; 
/* 189 */     synchronized (this) {
/* 190 */       if (this.noise == null || this.cachedSeed != seed) {
/* 191 */         this.noise = new SimpleNoise(seed);
/* 192 */         this.cachedSeed = seed;
/*     */       } 
/* 194 */       return this.noise;
/*     */     } 
/*     */   }
/*     */   
/*     */   private static double clamp01(double v) {
/* 199 */     return (v < 0.0D) ? 0.0D : Math.min(v, 1.0D);
/*     */   }
/*     */ }


/* Location:              /home/norbert/IdeaProjects/cpvpprac/target/CPVPSingleBiome-1.0.0.jar!/me/liass/cpvpsinglebiome/generator/SingleBiomeChunkGenerator.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */