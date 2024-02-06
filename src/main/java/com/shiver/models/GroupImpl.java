package com.shiver.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class GroupImpl implements Group {
    private final CharSequence groupId;
    private final CharSequence adminId;
    private final List<CharSequence> memberIds;
    private int groupIteration = 0;
    private final Date creationDate;

    public GroupImpl(CharSequence adminId, CharSequence groupId, Date creationDate) {
        this.adminId = adminId;
        this.creationDate = creationDate;

        this.groupId = groupId;

        this.memberIds = new ArrayList<>();
        memberIds.add(adminId);
    }

    @Override
    public CharSequence getGroupId() {
        return groupId;
    }

    @Override
    public CharSequence getAdminId() {
        return adminId;
    }

    @Override
    public List<CharSequence> getMemberIdList() {
        return new ArrayList<>(memberIds);
    }

    @Override
    public void addMember(CharSequence groupMember) {
        memberIds.add(groupMember);
        groupIteration++;
    }

    @Override
    public boolean removeMember(CharSequence groupMember) {
        boolean success = memberIds.remove(groupMember);
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
