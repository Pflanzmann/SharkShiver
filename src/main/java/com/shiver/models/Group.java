package com.shiver.models;

import java.util.Date;
import java.util.List;

public interface Group {
    CharSequence getGroupId();

    CharSequence getAdminId();

    List<CharSequence> getMemberIdList();

    void addMember(CharSequence groupMember);

    boolean removeMember(CharSequence groupMember);

    int getGroupIteration();

    Date getCreationDate();
}
