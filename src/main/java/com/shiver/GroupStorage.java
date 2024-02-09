package com.shiver;

import com.shiver.exceptions.ShiverNoGroupException;
import com.shiver.models.Group;
import com.shiver.models.Membership;

import java.util.HashMap;
import java.util.List;

public interface GroupStorage {
    void storeGroup(Group group);

    Group getGroup(CharSequence groupId) throws ShiverNoGroupException;

    List<Group> getAllGroups();

    HashMap<CharSequence, Membership> getAllOwnedMemberships();

    boolean hasMembershipForGroup(CharSequence groupId);

    boolean deleteGroup(CharSequence groupId);

    boolean hasGroup(CharSequence groupId);
}
