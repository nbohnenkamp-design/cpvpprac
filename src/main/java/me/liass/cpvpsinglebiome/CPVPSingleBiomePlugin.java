/*    */ package me.liass.cpvpsinglebiome;
/*    */ 
/*    */ import me.liass.cpvpsinglebiome.command.CPVPSBCommand;
/*    */ import me.liass.cpvpsinglebiome.command.CPVPSBTabCompleter;
/*    */ import me.liass.cpvpsinglebiome.config.ConfigManager;
/*    */ import me.liass.cpvpsinglebiome.generator.BiomeType;
/*    */ import me.liass.cpvpsinglebiome.generator.SingleBiomeChunkGenerator;
/*    */ import me.liass.cpvpsinglebiome.listener.WorldInitListener;
/*    */ import me.liass.cpvpsinglebiome.reset.ResetManager;
/*    */ import org.bukkit.command.CommandExecutor;
/*    */ import org.bukkit.command.PluginCommand;
/*    */ import org.bukkit.command.TabCompleter;
/*    */ import org.bukkit.event.Listener;
/*    */ import org.bukkit.generator.ChunkGenerator;
/*    */ import org.bukkit.plugin.Plugin;
/*    */ import org.bukkit.plugin.java.JavaPlugin;
/*    */ 
/*    */ 
/*    */ public class CPVPSingleBiomePlugin
/*    */   extends JavaPlugin
/*    */ {
/*    */   private ConfigManager configManager;
/*    */   private ResetManager resetManager;
/*    */   
/*    */   public void onLoad() {
/* 26 */     ensureConfigManager();
/*    */   }
/*    */ 
/*    */   
/*    */   public void onEnable() {
/* 31 */     ensureConfigManager();
/*    */     
/* 33 */     this.resetManager = new ResetManager(this, this.configManager);
/*    */     
/* 35 */     PluginCommand command = getCommand("cpvpsb");
/* 36 */     if (command != null) {
/* 37 */       command.setExecutor((CommandExecutor)new CPVPSBCommand(this, this.configManager, this.resetManager));
/* 38 */       command.setTabCompleter((TabCompleter)new CPVPSBTabCompleter());
/*    */     } 
/*    */     
/* 41 */     getServer().getPluginManager().registerEvents((Listener)new WorldInitListener(), (Plugin)this);
/*    */ 
/*    */ 
/*    */     
/* 45 */     getServer().getScheduler().runTask((Plugin)this, () -> this.resetManager.start());
/*    */     
/* 47 */     getLogger().info("CPVPSingleBiome v" + getDescription().getVersion() + " enabled.");
/* 48 */     getLogger().info("Default biome: " + this.configManager.getDefaultBiome());
/* 49 */     getLogger().info("Generator syntax: /mv create <world> normal -g CPVPSingleBiome:<biome>");
/*    */   }
/*    */ 
/*    */   
/*    */   public void onDisable() {
/* 54 */     if (this.resetManager != null) this.resetManager.stop(); 
/* 55 */     getLogger().info("CPVPSingleBiome disabled.");
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
/*    */     try {
/* 65 */       ensureConfigManager();
/* 66 */       BiomeType fallback = BiomeType.fromStringOrDefault(this.configManager
/* 67 */           .getDefaultBiome(), BiomeType.DESERT);
/* 68 */       BiomeType biomeType = BiomeType.fromStringOrDefault(id, fallback);
/* 69 */       return (ChunkGenerator)new SingleBiomeChunkGenerator(this.configManager, biomeType);
/* 70 */     } catch (Throwable t) {
/* 71 */       getLogger().severe("Failed to build generator for world '" + worldName + "' (id='" + id + "'): " + t
/* 72 */           .getClass().getSimpleName() + " - " + t
/* 73 */           .getMessage());
/*    */       
/* 75 */       ensureConfigManager();
/* 76 */       return (ChunkGenerator)new SingleBiomeChunkGenerator(this.configManager, BiomeType.DESERT);
/*    */     } 
/*    */   }
/*    */ 
/*    */   
/*    */   private synchronized void ensureConfigManager() {
/* 82 */     if (this.configManager == null) {
/*    */       try {
/* 84 */         saveDefaultConfig();
/* 85 */       } catch (Throwable throwable) {}
/*    */ 
/*    */       
/* 88 */       this.configManager = new ConfigManager(this);
/*    */     } 
/*    */   }
/*    */   
/*    */   public ConfigManager getConfigManager() {
/* 93 */     ensureConfigManager();
/* 94 */     return this.configManager;
/*    */   }
/*    */   
/*    */   public ResetManager getResetManager() {
/* 98 */     return this.resetManager;
/*    */   }
/*    */ }


/* Location:              /home/norbert/IdeaProjects/cpvpprac/target/CPVPSingleBiome-1.0.0.jar!/me/liass/cpvpsinglebiome/CPVPSingleBiomePlugin.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */