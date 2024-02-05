package com.shiver.models;

import java.util.Date;
import java.util.List;

public interface Group {
    GroupMember getAdmin();

    List<GroupMember> getMemberList();

    void addMember(GroupMember groupMember);

    boolean removeMember(GroupMember groupMember);

    int getGroupIteration();

    Date getCreationDate();
}
