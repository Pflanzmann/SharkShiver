package com.shiver;

import com.shiver.exceptions.NoGroupAvailableException;
import com.shiver.models.Group;

import java.util.List;

public interface GroupStorage {
    void storeGroup(Group group);

    Group getGroup(CharSequence groupId) throws NoGroupAvailableException;

    List<Group> getAllGroups() throws NoGroupAvailableException;

    boolean deleteGroup(CharSequence groupId);

    boolean hasGroup(CharSequence groupId);
}
