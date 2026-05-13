package eu.cpvpprac.practicebot;

import org.bukkit.Location;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;

/**
 * Repeating task (every tick) that drives follow movement and attack for all active bots.
 * All logic runs on the main thread via Bukkit's scheduler.
 */
public class BotTask extends BukkitRunnable {

    private final CPVPPracticeBotPlugin plugin;

    public BotTask(CPVPPracticeBotPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        ConfigManager cfg = plugin.getConfigManager();

        // Clamp to at least 1 tick between attacks; cap attackSpeed to avoid division issues
        int attackInterval = Math.max(1, (int) (20.0 / Math.max(0.05, cfg.attackSpeed)));

        for (Map.Entry<UUID, PracticeBot> entry : plugin.getBotManager().getAllBots().entrySet()) {
            PracticeBot bot = entry.getValue();
            Husk entity = bot.getEntity();

            if (!entity.isValid()) continue;

            Player owner = plugin.getServer().getPlayer(bot.getOwnerUuid());
            if (owner == null || !owner.isOnline()) continue;

            // Skip if owner is in a different world (e.g. after a world change)
            if (!owner.getWorld().equals(entity.getWorld())) continue;

            tickFollow(bot, entity, owner, cfg);
            tickAttack(bot, entity, owner, cfg, attackInterval);
        }
    }

    // -------------------------------------------------------------------------

    private void tickFollow(PracticeBot bot, Husk entity, Player owner, ConfigManager cfg) {
        if (!bot.isFollowEnabled()) return;

        bot.incrementFollowTick();
        // Check every 3 ticks to avoid teleporting every single tick
        if (bot.getFollowTickCounter() < 3) return;
        bot.resetFollowTick();

        Location entityLoc = entity.getLocation();
        Location ownerLoc  = owner.getLocation();
        double dist = entityLoc.distance(ownerLoc);

        if (dist <= cfg.followDistance) return;

        // Move toward owner by up to (speed × 3 ticks) per update
        Vector toOwner = ownerLoc.toVector().subtract(entityLoc.toVector());
        double step = Math.min(cfg.followMovementSpeed * 3.0, dist - cfg.followDistance);
        Vector movement = toOwner.normalize().multiply(step);

        double newX = entityLoc.getX() + movement.getX();
        double newZ = entityLoc.getZ() + movement.getZ();
        // Gradually close the vertical gap so the bot doesn't drift above/below terrain
        double newY = entityLoc.getY() + (ownerLoc.getY() - entityLoc.getY()) * 0.5;

        float yaw = yawToFace(entityLoc, ownerLoc);
        entity.teleport(new Location(entity.getWorld(), newX, newY, newZ, yaw, 0f));
    }

    private void tickAttack(PracticeBot bot, Husk entity, Player owner,
                            ConfigManager cfg, int attackInterval) {
        if (!bot.isAttackEnabled()) return;

        bot.incrementAttackTick();
        if (bot.getAttackTickCounter() < attackInterval) return;
        bot.resetAttackTick();

        double dist = entity.getLocation().distance(owner.getLocation());
        if (dist > cfg.attackReach) return;

        entity.swingMainHand();
        // Deal 1 heart of damage attributed to the bot
        owner.damage(2.0, entity);
    }

    /** Computes the yaw (degrees) for an entity at {@code from} to face {@code to}. */
    private float yawToFace(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }
}
