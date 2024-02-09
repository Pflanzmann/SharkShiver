package com.shiver;

import com.shiver.exceptions.ShiverNoGroupException;
import com.shiver.models.ShiverGroup;

import java.util.List;

/**
 * A simple dao interface for all information regarding groups and memberships
 */
public interface ShiverGroupStorage {

    /**
     * Method to create a new group.
     * When we create group objects only in the ShiverGroupStorage we enable that the developer can implement his own
     * group class with the [ShiverGroup] interface
     * [ShiverGroupImpl] is provided implementation
     *
     * @param groupId - id that the group should have to differenciate it with others. Should be something like a UUID
     * @param adminId - id of the admin
     * @return - returns the group object after it got created
     */
    ShiverGroup createNewGroup(CharSequence groupId, CharSequence adminId);

    /**
     * Parses the group from bytes nad returns the deserialized group object
     * It is not optimal to parse groups from the dao, but with that functionality we enable
     * that the developer can implement his own group object and use this storage to store them instead of shadowing or delegating to our structure
     * [ShiverGroupImpl] is provided implementation
     *
     * @param bytes
     * @return
     */
    ShiverGroup parseGroupFromBytes(byte[] bytes);

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
}
