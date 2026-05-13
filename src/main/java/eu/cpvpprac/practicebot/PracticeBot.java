package eu.cpvpprac.practicebot;

import org.bukkit.entity.Husk;

import java.util.UUID;

/**
 * Holds the runtime state for one player's practice bot.
 */
public class PracticeBot {

    private final UUID  ownerUuid;
    private final Husk  entity;
    private boolean     followEnabled;
    private boolean     attackEnabled;

    // Tick counters used by BotTask
    private int followTickCounter = 0;
    private int attackTickCounter = 0;

    public PracticeBot(UUID ownerUuid, Husk entity, boolean followEnabled, boolean attackEnabled) {
        this.ownerUuid     = ownerUuid;
        this.entity        = entity;
        this.followEnabled = followEnabled;
        this.attackEnabled = attackEnabled;
    }

    public UUID  getOwnerUuid()   { return ownerUuid; }
    public Husk  getEntity()      { return entity; }

    public boolean isFollowEnabled()                    { return followEnabled; }
    public void    setFollowEnabled(boolean v)          { followEnabled = v; }

    public boolean isAttackEnabled()                    { return attackEnabled; }
    public void    setAttackEnabled(boolean v)          { attackEnabled = v; }

    public int  getFollowTickCounter()  { return followTickCounter; }
    public void incrementFollowTick()   { followTickCounter++; }
    public void resetFollowTick()       { followTickCounter = 0; }

    public int  getAttackTickCounter()  { return attackTickCounter; }
    public void incrementAttackTick()   { attackTickCounter++; }
    public void resetAttackTick()       { attackTickCounter = 0; }
}
