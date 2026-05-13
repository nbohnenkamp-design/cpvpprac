package eu.cpvpprac.practicebot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityVelocityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Event handlers that keep bot behaviour clean and predictable:
 * - remove bot when the owner logs off
 * - prevent the bot from targeting anyone via vanilla AI
 * - absorb incoming damage when unlimited-health is on
 * - cancel knockback / explosion velocity when anti-knockback is on
 * - clear drops if the bot somehow dies
 */
public class BotListener implements Listener {

    private final CPVPPracticeBotPlugin plugin;

    public BotListener(CPVPPracticeBotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getBotManager().removeBot(event.getPlayer().getUniqueId());
    }

    /** Prevent the bot's disabled AI from picking a target through edge cases. */
    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (plugin.getBotManager().isBotEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /**
     * When unlimited-health is enabled, cancel all incoming damage.
     * The bot visually registers as an invincible target; clients still see
     * explosion effects around the entity even though no health is lost.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBotDamage(EntityDamageEvent event) {
        if (!plugin.getBotManager().isBotEntity(event.getEntity())) return;
        if (plugin.getConfigManager().unlimitedHealth) {
            event.setCancelled(true);
        }
    }

    /**
     * Safety net: if the bot somehow dies (unlimited-health off + fatal hit),
     * clear all drops, zero XP, and remove it from the manager.
     */
    @EventHandler
    public void onBotDeath(EntityDeathEvent event) {
        UUID ownerUuid = plugin.getBotManager().getOwnerByEntity(event.getEntity());
        if (ownerUuid == null) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        plugin.getBotManager().removeBot(ownerUuid);
    }

    /**
     * Cancel velocity changes caused by explosions or knockback.
     * Using teleport for follow movement means our own positional updates
     * never fire EntityVelocityEvent, so this only suppresses external forces.
     */
    @EventHandler(ignoreCancelled = true)
    public void onBotVelocity(EntityVelocityEvent event) {
        if (!plugin.getBotManager().isBotEntity(event.getEntity())) return;
        if (plugin.getConfigManager().antiKnockback) {
            event.setCancelled(true);
        }
    }
}
