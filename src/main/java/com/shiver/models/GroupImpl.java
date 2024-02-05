package com.shiver.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GroupImpl implements Group {
    private GroupMember admin = null;
    private List<GroupMember> members = null;
    private int groupIteration = 0;
    private Date creationDate = null;

    public GroupImpl(GroupMember admin, Date creationDate) {
        this.admin = admin;
        this.creationDate = creationDate;

        this.members = new ArrayList<>();
        members.add(admin);
    }

    @Override
    public GroupMember getAdmin() {
        return admin;
    }

    @Override
    public List<GroupMember> getMemberList() {
        return new ArrayList<>(members);
    }

    @Override
    public void addMember(GroupMember groupMember) {
        members.add(groupMember);
        groupIteration++;
    }

    @Override
    public boolean removeMember(GroupMember groupMember) {
        boolean success = members.remove(groupMember);
        if (success) {
            groupIteration++;
        }

        return success;
    }

    @Override
    public int getGroupIteration() {
        return groupIteration;
    }

    @Override
    public Date getCreationDate() {
        return new Date(creationDate.getTime());
    }
}
