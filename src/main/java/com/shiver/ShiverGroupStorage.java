package com.shiver;

import com.shiver.exceptions.ShiverNoGroupException;
import com.shiver.models.ShiverGroup;

import java.util.List;

/**
 * A simple dao interface for all information regarding groups and memberships
 */
public interface ShiverGroupStorage {

    /**
     * Store a group object
     *
     * @param group - group to store
     */
    void storeGroup(ShiverGroup group);

    /**
     * Get a group object
     *
     * @param groupId - group to retrieve
     * @return - a group object associated with the group id
     * @throws ShiverNoGroupException - throws if there is no group associated with the id
     */
    ShiverGroup getGroup(CharSequence groupId) throws ShiverNoGroupException;

    /**
     * @return - All groups that are stored
     */
    List<ShiverGroup> getAllGroups();

    /**
     * Deletes everything associated with group
     *
     * @param groupId - id of group to remove
     */
    void deleteGroup(CharSequence groupId);

    /**
     * Checks if the provided memberId is the admin of the group
     *
     * @param groupId  - id of the group to check
     * @param memberId - id of member to check against
     * @return - true if member is member, false if not or if the group does not exist
     */
    boolean isAdminOfGroup(CharSequence groupId, CharSequence memberId);

    /**
     * @param groupId = id to look for
     * @return false if there is no group associated with the id
     */
    boolean hasGroupWithId(CharSequence groupId);
}
