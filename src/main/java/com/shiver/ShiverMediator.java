package com.shiver;

import com.shiver.exceptions.ShiverNotSyncableException;
import com.shiver.models.ShiverGroup;

/**
 * This interface is used to mediate/ sync groups with each other.
 * If we receive a group update we have to decide how to merge those or what group to keep. This interface helps with doing so.
 */
public interface ShiverMediator {
    /**
     * This method gets called to mediate between two groups and resolve conflicts
     *
     * @param originalGroup - the group that was previously set
     * @param newGroup - the new group or group update
     * @return - the done synced group. This can be an instance of the two put in groups or a whole new one.
     * Either way should only the returned instance used from here on. This should not be null.
     * if it can not get resolved then throw an exception
     * @throws ShiverNotSyncableException - This should get thrown if the conflict can not get resolved and no group can get returned
     */
    ShiverGroup mediate(ShiverGroup originalGroup, ShiverGroup newGroup) throws ShiverNotSyncableException;
}
