package com.shiver;

import com.shiver.models.ShiverGroup;

public interface ShiverGroupFactory {

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
     * Parses the group from bytes and returns the deserialized group object
     * It is not optimal to parse groups from a factory, but with that functionality we enable
     * that the developer can implement his own group object and use this factory to store them instead
     * of shadowing or delegating to our structures above this library
     * [ShiverGroupImpl] is provided implementation
     *
     * @param bytes
     * @return
     */
    ShiverGroup parseGroupFromBytes(byte[] bytes);
}
