package com.shiver;

import com.shiver.exceptions.NoGroupAvailableException;
import com.shiver.models.Group;

public interface SingleGroupStorage {
    void storeGroup(Group group);

    Group getGroup() throws NoGroupAvailableException;

    boolean deleteGroup();

    boolean hasGroup();
}
