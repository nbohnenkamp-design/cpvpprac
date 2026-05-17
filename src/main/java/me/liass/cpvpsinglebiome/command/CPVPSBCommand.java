/*     */ package me.liass.cpvpsinglebiome.command;
/*     */ 
/*     */ import java.time.LocalDate;
/*     */ import java.time.LocalDateTime;
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ import me.liass.cpvpsinglebiome.CPVPSingleBiomePlugin;
/*     */ import me.liass.cpvpsinglebiome.chunky.ChunkyIntegration;
/*     */ import me.liass.cpvpsinglebiome.config.ConfigManager;
/*     */ import me.liass.cpvpsinglebiome.generator.BiomeType;
/*     */ import me.liass.cpvpsinglebiome.generator.SingleBiomeChunkGenerator;
/*     */ import me.liass.cpvpsinglebiome.listener.WorldInitListener;
/*     */ import me.liass.cpvpsinglebiome.reset.ResetManager;
/*     */ import me.liass.cpvpsinglebiome.reset.ResetWorldSpec;
/*     */ import org.bukkit.Bukkit;
/*     */ import org.bukkit.World;
/*     */ import org.bukkit.WorldCreator;
/*     */ import org.bukkit.command.Command;
/*     */ import org.bukkit.command.CommandExecutor;
/*     */ import org.bukkit.command.CommandSender;
/*     */ import org.bukkit.entity.Player;
/*     */ import org.bukkit.generator.ChunkGenerator;
/*     */ import org.bukkit.plugin.Plugin;
/*     */ 
/*     */ public class CPVPSBCommand implements CommandExecutor {
/*     */   private static final String HEADER = "§5§l━━━━━━━━━━━━━━ §dCPVPSingleBiome §5§l━━━━━━━━━━━━━━";
/*     */   private static final String FOOTER = "§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
/*     */   private final CPVPSingleBiomePlugin plugin;
/*     */   private final ConfigManager config;
/*     */   private final ResetManager resetManager;
/*     */   
/*     */   public CPVPSBCommand(CPVPSingleBiomePlugin plugin, ConfigManager config, ResetManager resetManager) {
/*  33 */     this.plugin = plugin;
/*  34 */     this.config = config;
/*  35 */     this.resetManager = resetManager;
/*     */   }
/*     */ 
/*     */   
/*     */   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
/*  40 */     if (args.length == 0) {
/*  41 */       sendHelp(sender);
/*  42 */       return true;
/*     */     } 
/*     */     
/*  45 */     switch (args[0].toLowerCase()) { case "help":
/*  46 */         sendHelp(sender);
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
/*  60 */         return true;case "reload": handleReload(sender); return true;case "biomes": handleBiomes(sender); return true;case "info": handleInfo(sender); return true;case "create": handleCreate(sender, args); return true;case "tp": handleTp(sender, args); return true;case "reset": handleReset(sender, args); return true;case "chunky": handleChunky(sender, args); return true; }  sender.sendMessage(this.config.getPrefix() + "§cUnknown subcommand. Use §f/cpvpsb help §cfor a list of commands."); sendHelp(sender); return true;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void sendHelp(CommandSender sender) {
/*  68 */     sender.sendMessage("§5§l━━━━━━━━━━━━━━ §dCPVPSingleBiome §5§l━━━━━━━━━━━━━━");
/*  69 */     sender.sendMessage("§d  /cpvpsb help                   §f» Show this help");
/*  70 */     sender.sendMessage("§d  /cpvpsb reload                 §f» Reload config.yml");
/*  71 */     sender.sendMessage("§d  /cpvpsb biomes                 §f» List available biomes");
/*  72 */     sender.sendMessage("§d  /cpvpsb info                   §f» Show plugin info");
/*  73 */     sender.sendMessage("§d  /cpvpsb create <world> [biome] §f» Create a single-biome world");
/*  74 */     sender.sendMessage("§d  /cpvpsb tp <world>             §f» Teleport to a world");
/*  75 */     sender.sendMessage("§d  /cpvpsb reset now              §f» Reset all configured arenas now");
/*  76 */     sender.sendMessage("§d  /cpvpsb reset <world>          §f» Reset a single configured arena");
/*  77 */     sender.sendMessage("§d  /cpvpsb reset status           §f» Show reset/chunky configuration");
/*  78 */     sender.sendMessage("§d  /cpvpsb reset reload           §f» Reload config and re-arm scheduler");
/*  79 */     sender.sendMessage("§d  /cpvpsb chunky start <world>   §f» Start Chunky for one world");
/*  80 */     sender.sendMessage("§d  /cpvpsb chunky start-all       §f» Start Chunky for all reset worlds");
/*  81 */     sender.sendMessage("");
/*  82 */     sender.sendMessage("§5  Available biomes: §f" + BiomeType.getNames());
/*  83 */     sender.sendMessage("§5  Multiverse syntax: §f/mv create <world> normal -g CPVPSingleBiome:<biome>");
/*  84 */     sender.sendMessage("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
/*     */   }
/*     */   
/*     */   private void handleReload(CommandSender sender) {
/*  88 */     if (!hasPermission(sender, "cpvpsinglebiome.reload"))
/*     */       return;  try {
/*  90 */       this.config.reload();
/*  91 */       if (this.resetManager != null) this.resetManager.reload(); 
/*  92 */       sender.sendMessage(this.config.getPrefix() + "§aConfiguration reloaded successfully.");
/*  93 */     } catch (Throwable t) {
/*  94 */       sender.sendMessage(this.config.getPrefix() + "§cReload failed: §f" + this.config.getPrefix());
/*  95 */       this.plugin.getLogger().warning("Reload failed: " + String.valueOf(t));
/*     */     } 
/*     */   }
/*     */   
/*     */   private void handleBiomes(CommandSender sender) {
/* 100 */     if (!hasPermission(sender, "cpvpsinglebiome.info"))
/* 101 */       return;  sender.sendMessage(this.config.getPrefix() + "§fAvailable biomes:");
/* 102 */     for (BiomeType type : BiomeType.values()) {
/* 103 */       sender.sendMessage("  §5» §d" + type.getId());
/*     */     }
/*     */   }
/*     */   
/*     */   private void handleInfo(CommandSender sender) {
/* 108 */     if (!hasPermission(sender, "cpvpsinglebiome.info"))
/* 109 */       return;  sender.sendMessage("§5§l━━━━━━━━━━━━━━ §dCPVPSingleBiome §5§l━━━━━━━━━━━━━━");
/* 110 */     sender.sendMessage("§d  Version:          §f" + this.plugin.getDescription().getVersion());
/* 111 */     sender.sendMessage("§d  Default biome:    §f" + this.config.getDefaultBiome());
/* 112 */     sender.sendMessage("§d  Base height:      §f" + this.config.getBaseHeight());
/* 113 */     sender.sendMessage("§d  Height variation: §f" + this.config.getHeightVariation());
/* 114 */     sender.sendMessage("§d  Noise scale:      §f" + this.config.getNoiseScale());
/* 115 */     sender.sendMessage("§d  Flatness:         §f" + this.config.getFlatness());
/* 116 */     sender.sendMessage("§d  World border:     §f" + (
/* 117 */         (this.config.getWorldBorderSize() > 0.0D) ? ("" + this.config.getWorldBorderSize() + " blocks") : "disabled"));
/* 118 */     sender.sendMessage("§d  Decorations:      §f" + (
/* 119 */         this.config.isDecorationEnabled() ? ("enabled (density " + 
/* 120 */         this.config.getDecorationDensity() + ")") : 
/* 121 */         "disabled"));
/* 122 */     sender.sendMessage("§d  Auto-reset:       §f" + (
/* 123 */         this.config.isResetEnabled() ? ("enabled @ " + 
/* 124 */         String.valueOf(this.config.getResetTime()) + " " + this.config.getResetZone().getId()) : 
/* 125 */         "disabled"));
/* 126 */     sender.sendMessage("");
/* 127 */     sender.sendMessage("§5  Multiverse syntax: §f/mv create <world> normal -g CPVPSingleBiome:<biome>");
/* 128 */     sender.sendMessage("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void handleCreate(CommandSender sender, String[] args) {
/*     */     World world;
/* 136 */     if (!hasPermission(sender, "cpvpsinglebiome.create"))
/* 137 */       return;  if (args.length < 2) {
/* 138 */       sender.sendMessage(this.config.getPrefix() + "§cUsage: §f/cpvpsb create <worldname> [biome]");
/*     */       
/*     */       return;
/*     */     } 
/* 142 */     String worldName = args[1];
/* 143 */     String biomeName = (args.length >= 3) ? args[2] : this.config.getDefaultBiome();
/*     */     
/* 145 */     BiomeType biomeType = BiomeType.fromString(biomeName);
/* 146 */     if (biomeType == null) {
/* 147 */       sender.sendMessage(this.config.getPrefix() + "§cUnknown biome: §f" + this.config.getPrefix());
/* 148 */       sender.sendMessage(this.config.getPrefix() + "§cAvailable biomes: §f" + this.config.getPrefix());
/*     */       
/*     */       return;
/*     */     } 
/* 152 */     if (Bukkit.getWorld(worldName) != null) {
/* 153 */       sender.sendMessage(this.config.getPrefix() + "§cA world named §f" + this.config.getPrefix() + "§c already exists.");
/*     */       
/*     */       return;
/*     */     } 
/* 157 */     sender.sendMessage(this.config.getPrefix() + "§fCreating world §d" + this.config.getPrefix() + "§f with biome §d" + worldName + "§f...");
/*     */ 
/*     */ 
/*     */     
/*     */     try {
/* 162 */       WorldCreator creator = new WorldCreator(worldName);
/* 163 */       creator.generator((ChunkGenerator)new SingleBiomeChunkGenerator(this.config, biomeType));
/* 164 */       creator.generateStructures(false);
/* 165 */       world = creator.createWorld();
/* 166 */     } catch (Throwable t) {
/* 167 */       sender.sendMessage(this.config.getPrefix() + "§cFailed to create world: §f" + this.config.getPrefix());
/* 168 */       this.plugin.getLogger().severe("World creation failed for '" + worldName + "': " + String.valueOf(t));
/*     */       
/*     */       return;
/*     */     } 
/* 172 */     if (world == null) {
/* 173 */       sender.sendMessage(this.config.getPrefix() + "§cFailed to create world §f" + this.config.getPrefix() + "§c.");
/*     */       
/*     */       return;
/*     */     } 
/* 177 */     WorldInitListener.applyArenaRules(world);
/*     */     
/* 179 */     double borderSize = this.config.getWorldBorderSize();
/* 180 */     if (borderSize > 0.0D) {
/* 181 */       world.getWorldBorder().setSize(borderSize);
/* 182 */       world.getWorldBorder().setCenter(world.getSpawnLocation());
/*     */     } 
/*     */     
/* 185 */     sender.sendMessage(this.config.getPrefix() + "§aWorld §f" + this.config.getPrefix() + "§a created with biome §f" + worldName + "§a!");
/*     */     
/* 187 */     sender.sendMessage(this.config.getPrefix() + "§fUse §d/cpvpsb tp " + this.config.getPrefix() + "§f to teleport there.");
/*     */   }
/*     */   
/*     */   private void handleTp(CommandSender sender, String[] args) {
/*     */     Player player;
/* 192 */     if (sender instanceof Player) { player = (Player)sender; }
/* 193 */     else { sender.sendMessage(this.config.getPrefix() + "§cThis command can only be used by players.");
/*     */       return; }
/*     */     
/* 196 */     if (!hasPermission((CommandSender)player, "cpvpsinglebiome.tp"))
/* 197 */       return;  if (args.length < 2) {
/* 198 */       sender.sendMessage(this.config.getPrefix() + "§cUsage: §f/cpvpsb tp <worldname>");
/*     */       
/*     */       return;
/*     */     } 
/* 202 */     String worldName = args[1];
/* 203 */     World world = Bukkit.getWorld(worldName);
/* 204 */     if (world == null) {
/* 205 */       sender.sendMessage(this.config.getPrefix() + "§cWorld §f" + this.config.getPrefix() + "§c not found or not loaded.");
/*     */       
/*     */       return;
/*     */     } 
/*     */     
/* 210 */     player.teleport(world.getSpawnLocation());
/* 211 */     player.sendMessage(this.config.getPrefix() + "§aTeleported to §f" + this.config.getPrefix() + "§a!");
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private void handleReset(CommandSender sender, String[] args) {
/* 219 */     if (args.length < 2) {
/* 220 */       sender.sendMessage(this.config.getPrefix() + "§cUsage: §f/cpvpsb reset <now|status|reload|worldname>");
/*     */       
/*     */       return;
/*     */     } 
/* 224 */     if (this.resetManager == null) {
/* 225 */       sender.sendMessage(this.config.getPrefix() + "§cReset manager is not available.");
/*     */       
/*     */       return;
/*     */     } 
/* 229 */     String sub = args[1].toLowerCase();
/* 230 */     switch (sub) {
/*     */       case "now":
/* 232 */         if (!hasPermission(sender, "cpvpsinglebiome.reset.now"))
/* 233 */           return;  if (this.resetManager.isResetInProgress()) {
/* 234 */           sender.sendMessage(this.config.getPrefix() + "§cA reset is already in progress.");
/*     */           return;
/*     */         } 
/* 237 */         this.resetManager.resetAll(sender);
/*     */         return;
/*     */       case "status":
/* 240 */         if (!hasPermission(sender, "cpvpsinglebiome.reset"))
/* 241 */           return;  sendResetStatus(sender);
/*     */         return;
/*     */       case "reload":
/* 244 */         if (!hasPermission(sender, "cpvpsinglebiome.reload"))
/*     */           return;  try {
/* 246 */           this.config.reload();
/* 247 */           this.resetManager.reload();
/* 248 */           sender.sendMessage(this.config.getPrefix() + "§aReset configuration reloaded.");
/* 249 */         } catch (Throwable t) {
/* 250 */           sender.sendMessage(this.config.getPrefix() + "§cReload failed: §f" + this.config.getPrefix());
/*     */         } 
/*     */         return;
/*     */     } 
/* 254 */     if (!hasPermission(sender, "cpvpsinglebiome.reset.now"))
/* 255 */       return;  if (this.resetManager.isResetInProgress()) {
/* 256 */       sender.sendMessage(this.config.getPrefix() + "§cA reset is already in progress.");
/*     */       return;
/*     */     } 
/* 259 */     this.resetManager.resetWorldByName(args[1], sender);
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private void sendResetStatus(CommandSender sender) {
/* 265 */     sender.sendMessage("§5§l━━━━━━━━━━━━━━ §dCPVPSingleBiome §5§l━━━━━━━━━━━━━━");
/* 266 */     sender.sendMessage("§d  Auto-reset:      §f" + (
/* 267 */         this.config.isResetEnabled() ? "enabled" : "disabled"));
/* 268 */     sender.sendMessage("§d  Reset time:      §f" + String.valueOf(this.config.getResetTime()) + " §7(" + this.config
/* 269 */         .getResetZone().getId() + ")");
/* 270 */     sender.sendMessage("§d  Interval:        §f" + this.config.getResetIntervalDays() + " day(s)");
/*     */     
/* 272 */     LocalDate lastReset = this.resetManager.getLastAutoResetDate();
/* 273 */     sender.sendMessage("§d  Last auto reset: §f" + (
/* 274 */         (lastReset != null) ? lastReset.toString() : "never"));
/* 275 */     LocalDateTime next = this.resetManager.getNextResetAt();
/* 276 */     sender.sendMessage("§d  Next auto reset: §f" + String.valueOf((next != null) ? next : "—"));
/* 277 */     sender.sendMessage("§d  Fallback world:  §f" + this.config.getFallbackWorld());
/* 278 */     sender.sendMessage("§d  Skip if online:  §f" + this.config.isSkipIfAnyPlayerOnline());
/* 279 */     sender.sendMessage("§d  Warnings:        §f" + (
/* 280 */         this.config.isWarningEnabled() ? (String.valueOf(this.config.getWarningMinutes()) + " min") : "disabled"));
/* 281 */     sender.sendMessage("§d  Backup folder:   §f" + this.config.isBackupOldWorlds());
/* 282 */     sender.sendMessage("§d  Delete on OK:    §f" + this.config.isDeleteOldWorldsAfterSuccess());
/* 283 */     sender.sendMessage("§d  Configured worlds:");
/* 284 */     List<ResetWorldSpec> specs = this.config.getResetWorlds();
/* 285 */     if (specs.isEmpty()) {
/* 286 */       sender.sendMessage("    §7(none)");
/*     */     } else {
/* 288 */       for (ResetWorldSpec s : specs) {
/* 289 */         sender.sendMessage("    §5» §d" + s.worldName() + " §7(" + s.biomeName() + ")");
/*     */       }
/*     */     } 
/* 292 */     sender.sendMessage("§d  Chunky:          §f" + (
/* 293 */         this.config.isChunkyEnabled() ? "enabled" : "disabled") + " §7(installed: " + 
/* 294 */         ChunkyIntegration.isAvailable() + ")");
/* 295 */     if (this.config.isChunkyEnabled()) {
/* 296 */       sender.sendMessage("    §7radius=" + this.config.getChunkyRadius() + " shape=" + this.config
/* 297 */           .getChunkyShape() + " center-spawn=" + this.config
/* 298 */           .isChunkyCenterSpawn() + " silent=" + this.config
/* 299 */           .isChunkySilent() + " delay=" + this.config
/* 300 */           .getChunkyDelaySeconds() + "s");
/*     */     }
/* 302 */     sender.sendMessage("§5§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
/*     */   }
/*     */ 
/*     */   
/*     */   private void handleChunky(CommandSender sender, String[] args) {
/*     */     World w;
/*     */     List<String> names;
/*     */     boolean ok;
/* 310 */     if (!hasPermission(sender, "cpvpsinglebiome.chunky"))
/* 311 */       return;  if (args.length < 2) {
/* 312 */       sender.sendMessage(this.config.getPrefix() + "§cUsage: §f/cpvpsb chunky <start <world>|start-all>");
/*     */       
/*     */       return;
/*     */     } 
/* 316 */     String sub = args[1].toLowerCase();
/* 317 */     switch (sub) {
/*     */       case "start":
/* 319 */         if (args.length < 3) {
/* 320 */           sender.sendMessage(this.config.getPrefix() + "§cUsage: §f/cpvpsb chunky start <world>");
/*     */           return;
/*     */         } 
/* 323 */         w = Bukkit.getWorld(args[2]);
/* 324 */         if (w == null) {
/* 325 */           sender.sendMessage(this.config.getPrefix() + "§cWorld §f" + this.config.getPrefix() + "§c not found.");
/*     */           return;
/*     */         } 
/* 328 */         ok = ChunkyIntegration.start((Plugin)this.plugin, w, this.config);
/* 329 */         sender.sendMessage(this.config.getPrefix() + this.config.getPrefix());
/*     */         return;
/*     */ 
/*     */       
/*     */       case "start-all":
/* 334 */         names = new ArrayList<>();
/* 335 */         for (ResetWorldSpec s : this.config.getResetWorlds()) names.add(s.worldName()); 
/* 336 */         if (names.isEmpty()) {
/* 337 */           sender.sendMessage(this.config.getPrefix() + "§eNo reset worlds configured.");
/*     */           return;
/*     */         } 
/* 340 */         if (!ChunkyIntegration.isAvailable()) {
/* 341 */           sender.sendMessage(this.config.getPrefix() + "§cChunky is not installed.");
/*     */           return;
/*     */         } 
/* 344 */         ChunkyIntegration.startAll((Plugin)this.plugin, names, this.config);
/* 345 */         sender.sendMessage(this.config.getPrefix() + "§aChunky scheduled for §f" + this.config.getPrefix() + "§a worlds (delay " + names
/* 346 */             .size() + "s between starts).");
/*     */         return;
/*     */     } 
/* 349 */     sender.sendMessage(this.config.getPrefix() + "§cUnknown chunky subcommand. Use §fstart §cor §fstart-all§c.");
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   private boolean hasPermission(CommandSender sender, String permission) {
/* 359 */     if (sender.hasPermission("cpvpsinglebiome.admin") || sender.hasPermission(permission)) {
/* 360 */       return true;
/*     */     }
/* 362 */     sender.sendMessage(this.config.getPrefix() + "§cYou don't have permission to do that.");
/* 363 */     return false;
/*     */   }
/*     */ }


/* Location:              /home/norbert/IdeaProjects/cpvpprac/target/CPVPSingleBiome-1.0.0.jar!/me/liass/cpvpsinglebiome/command/CPVPSBCommand.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */