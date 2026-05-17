/*    */ package me.liass.cpvpsinglebiome.listener;
/*    */ 
/*    */ import org.bukkit.GameRule;
/*    */ import org.bukkit.World;
/*    */ import org.bukkit.event.EventHandler;
/*    */ import org.bukkit.event.EventPriority;
/*    */ import org.bukkit.event.Listener;
/*    */ import org.bukkit.event.world.WorldInitEvent;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class WorldInitListener
/*    */   implements Listener
/*    */ {
/*    */   @EventHandler(priority = EventPriority.MONITOR)
/*    */   public void onWorldInit(WorldInitEvent event) {
/* 19 */     World world = event.getWorld();
/* 20 */     if (!(world.getGenerator() instanceof me.liass.cpvpsinglebiome.generator.SingleBiomeChunkGenerator))
/*    */       return; 
/* 22 */     applyArenaRules(world);
/*    */   }
/*    */ 
/*    */   
/*    */   public static void applyArenaRules(World world) {
/* 27 */     world.setSpawnFlags(false, false);
/* 28 */     world.setGameRule(GameRule.DO_MOB_SPAWNING, Boolean.valueOf(false));
/* 29 */     world.setGameRule(GameRule.DO_TRADER_SPAWNING, Boolean.valueOf(false));
/* 30 */     world.setGameRule(GameRule.DO_PATROL_SPAWNING, Boolean.valueOf(false));
/* 31 */     world.setGameRule(GameRule.DO_WEATHER_CYCLE, Boolean.valueOf(false));
/* 32 */     world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, Boolean.valueOf(false));
/* 33 */     world.setGameRule(GameRule.DO_FIRE_TICK, Boolean.valueOf(false));
/* 34 */     world.setStorm(false);
/* 35 */     world.setThundering(false);
/*    */   }
/*    */ }


/* Location:              /home/norbert/IdeaProjects/cpvpprac/target/CPVPSingleBiome-1.0.0.jar!/me/liass/cpvpsinglebiome/listener/WorldInitListener.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */