/*    */ package me.liass.cpvpsinglebiome.generator;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public final class SimpleNoise
/*    */ {
/*    */   private final long seed;
/*    */   
/*    */   public SimpleNoise(long seed) {
/* 17 */     this.seed = seed;
/*    */   }
/*    */ 
/*    */   
/*    */   private double hash(int x, int z) {
/* 22 */     long h = x * 374761393L + z * 668265263L + this.seed * 1442695040888963407L;
/* 23 */     h = (h ^ h >>> 13L) * 1274126177L;
/* 24 */     h ^= h >>> 16L;
/* 25 */     return (h & 0x7FFFFFFFL) / 2.147483647E9D * 2.0D - 1.0D;
/*    */   }
/*    */ 
/*    */   
/*    */   public double noise(double x, double z) {
/* 30 */     int xi = (int)Math.floor(x);
/* 31 */     int zi = (int)Math.floor(z);
/* 32 */     double xf = x - xi;
/* 33 */     double zf = z - zi;
/*    */ 
/*    */     
/* 36 */     double sx = xf * xf * (3.0D - 2.0D * xf);
/* 37 */     double sz = zf * zf * (3.0D - 2.0D * zf);
/*    */     
/* 39 */     double v00 = hash(xi, zi);
/* 40 */     double v10 = hash(xi + 1, zi);
/* 41 */     double v01 = hash(xi, zi + 1);
/* 42 */     double v11 = hash(xi + 1, zi + 1);
/*    */     
/* 44 */     double a = v00 + sx * (v10 - v00);
/* 45 */     double b = v01 + sx * (v11 - v01);
/* 46 */     return a + sz * (b - a);
/*    */   }
/*    */ 
/*    */   
/*    */   public double octaveNoise(double x, double z, int octaves, double persistence) {
/* 51 */     double total = 0.0D;
/* 52 */     double amplitude = 1.0D;
/* 53 */     double frequency = 1.0D;
/* 54 */     double maxValue = 0.0D;
/* 55 */     for (int i = 0; i < octaves; i++) {
/* 56 */       total += noise(x * frequency, z * frequency) * amplitude;
/* 57 */       maxValue += amplitude;
/* 58 */       amplitude *= persistence;
/* 59 */       frequency *= 2.0D;
/*    */     } 
/* 61 */     return total / maxValue;
/*    */   }
/*    */ }


/* Location:              /home/norbert/IdeaProjects/cpvpprac/target/CPVPSingleBiome-1.0.0.jar!/me/liass/cpvpsinglebiome/generator/SimpleNoise.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */