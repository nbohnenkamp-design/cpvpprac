/*    */ package me.liass.cpvpsinglebiome.command;
/*    */ 
/*    */ import java.util.Arrays;
/*    */ import java.util.List;
/*    */ import java.util.stream.Collectors;
/*    */ import me.liass.cpvpsinglebiome.generator.BiomeType;
/*    */ import org.bukkit.Bukkit;
/*    */ import org.bukkit.command.Command;
/*    */ import org.bukkit.command.CommandSender;
/*    */ import org.bukkit.command.TabCompleter;
/*    */ import org.bukkit.generator.WorldInfo;
/*    */ 
/*    */ 
/*    */ public class CPVPSBTabCompleter
/*    */   implements TabCompleter
/*    */ {
/* 17 */   private static final List<String> SUBCOMMANDS = Arrays.asList(new String[] { "help", "reload", "biomes", "info", "create", "tp", "reset", "chunky" });
/*    */ 
/*    */   
/* 20 */   private static final List<String> RESET_SUBS = Arrays.asList(new String[] { "now", "status", "reload" });
/*    */ 
/*    */   
/* 23 */   private static final List<String> CHUNKY_SUBS = Arrays.asList(new String[] { "start", "start-all" });
/*    */   
/* 25 */   private static final List<String> BIOME_NAMES = (List<String>)Arrays.<BiomeType>stream(BiomeType.values())
/* 26 */     .map(BiomeType::getId)
/* 27 */     .collect(Collectors.toList());
/*    */ 
/*    */   
/*    */   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
/* 31 */     if (args.length == 1) {
/* 32 */       return filterPrefix(SUBCOMMANDS, args[0]);
/*    */     }
/*    */     
/* 35 */     if (args.length == 2) {
/* 36 */       switch (args[0].toLowerCase()) { case "tp": case "create": case "reset": case "chunky":  }  return 
/*    */ 
/*    */ 
/*    */ 
/*    */         
/* 41 */         List.of();
/*    */     } 
/*    */ 
/*    */     
/* 45 */     if (args.length == 3) {
/* 46 */       if (args[0].equalsIgnoreCase("create")) {
/* 47 */         return filterPrefix(BIOME_NAMES, args[2]);
/*    */       }
/* 49 */       if (args[0].equalsIgnoreCase("chunky") && args[1].equalsIgnoreCase("start")) {
/* 50 */         return filterPrefix(getLoadedWorldNames(), args[2]);
/*    */       }
/*    */     } 
/*    */     
/* 54 */     return List.of();
/*    */   }
/*    */   
/*    */   private List<String> filterPrefix(List<String> options, String prefix) {
/* 58 */     String lowerPrefix = prefix.toLowerCase();
/* 59 */     return (List<String>)options.stream()
/* 60 */       .filter(s -> s.toLowerCase().startsWith(lowerPrefix))
/* 61 */       .collect(Collectors.toList());
/*    */   }
/*    */   
/*    */   private List<String> getLoadedWorldNames() {
/* 65 */     return (List<String>)Bukkit.getWorlds().stream()
/* 66 */       .map(WorldInfo::getName)
/* 67 */       .collect(Collectors.toList());
/*    */   }
/*    */ }


/* Location:              /home/norbert/IdeaProjects/cpvpprac/target/CPVPSingleBiome-1.0.0.jar!/me/liass/cpvpsinglebiome/command/CPVPSBTabCompleter.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */