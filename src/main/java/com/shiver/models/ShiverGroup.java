package com.shiver.models;

import java.util.List;

/**
 * Interface to implement to represent groups in the shiver library
 */
public interface ShiverGroup {

    /**
     * Should be a UUID
     *
     * @return id associated with the group
     */
    CharSequence getGroupId();

    /**
     * @return - id of the admin of the group
     */
    CharSequence getAdminId();

    /**
     * @return - list of all members
     */
    List<CharSequence> getMemberIdList();

    /**
     * Adds a member to the group
     *
     * @param groupMember - id of the member to add
     */
    void addMember(CharSequence groupMember);

    /**
     * Removes a member from the group
     *
     * @param groupMember - id of the member to add
     * @return - true if success, false if nothing changed
     */
    boolean removeMember(CharSequence groupMember);

    /**
     * A group iteration should get increased every time the group gets changed
     * This helps to know what groupUpdate is the newest
     *
     * @return - integer representing the version
     */
    int getGroupIteration();

    /**
     * A method to serialize the group object to be able to serialize it without an actual implementation
     *
     * @return - bytes that are the result of this serialization
     */
    byte[] serialize();
}
