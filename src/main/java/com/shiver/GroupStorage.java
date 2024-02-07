package com.shiver;

import com.shiver.exceptions.ShiverNoGroupException;
import com.shiver.models.Group;

import java.util.List;

public interface GroupStorage {
    void storeGroup(Group group);

    Group getGroup(CharSequence groupId) throws ShiverNoGroupException;

    List<Group> getAllGroups();

    boolean deleteGroup(CharSequence groupId);

    boolean hasGroup(CharSequence groupId);
}
