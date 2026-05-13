package eu.cpvpprac.practicebot;

import org.bukkit.entity.Husk;

import java.util.UUID;

/**
 * Holds the runtime state for one player's practice bot.
 *
 * Original fields and methods are unchanged.
 * currentKit tracking is a new addition for the kit preset system.
 */
public class PracticeBot {

    private final UUID    ownerUuid;
    private final Husk    entity;
    private boolean       followEnabled;
    private boolean       attackEnabled;

    // Tick counters used by BotTask
    private int followTickCounter = 0;
    private int attackTickCounter = 0;

    // -------------------------------------------------------------------------
    // Kit tracking — new
    // -------------------------------------------------------------------------

    /**
     * The name of the currently applied kit preset (lower-case, matches a key
     * in ConfigManager.getKits()).  Empty string means no kit has been applied
     * via the kit system (e.g. spawned with legacy ArmorUtil.applyArmor).
     */
    private String currentKit = "";

    // -------------------------------------------------------------------------

    public PracticeBot(UUID ownerUuid, Husk entity,
                       boolean followEnabled, boolean attackEnabled) {
        this.ownerUuid     = ownerUuid;
        this.entity        = entity;
        this.followEnabled = followEnabled;
        this.attackEnabled = attackEnabled;
    }

    // -------------------------------------------------------------------------
    // Original accessors — unchanged
    // -------------------------------------------------------------------------

    public UUID  getOwnerUuid()  { return ownerUuid; }
    public Husk  getEntity()     { return entity; }

    public boolean isFollowEnabled()           { return followEnabled; }
    public void    setFollowEnabled(boolean v) { followEnabled = v; }

    public boolean isAttackEnabled()           { return attackEnabled; }
    public void    setAttackEnabled(boolean v) { attackEnabled = v; }

    public int  getFollowTickCounter() { return followTickCounter; }
    public void incrementFollowTick()  { followTickCounter++; }
    public void resetFollowTick()      { followTickCounter = 0; }

    public int  getAttackTickCounter() { return attackTickCounter; }
    public void incrementAttackTick()  { attackTickCounter++; }
    public void resetAttackTick()      { attackTickCounter = 0; }

    // -------------------------------------------------------------------------
    // Kit accessors — new
    // -------------------------------------------------------------------------

    /** Returns the lower-case name of the currently applied kit, or "" if none. */
    public String getCurrentKit()           { return currentKit; }

    /** Records the kit name after it has been applied to the entity. */
    public void   setCurrentKit(String kit) { this.currentKit = kit == null ? "" : kit.toLowerCase(); }
}
