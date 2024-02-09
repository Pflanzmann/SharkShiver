package com.shiver.models;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple implementation for the [ShiverGroup] interface
 * To serialize and deserialize we use the GSON library
 */
public class ShiverGroupImpl implements ShiverGroup {
    private final CharSequence groupId;
    private final CharSequence adminId;
    private final List<CharSequence> memberIds;
    private int groupIteration = 0;

    public ShiverGroupImpl(CharSequence adminId, CharSequence groupId) {
        this.adminId = adminId;

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
    public byte[] serialize() {
        return new Gson().toJson(this, ShiverGroupImpl.class).getBytes();
    }

    public static ShiverGroup deserialize(byte[] data) {
        return new Gson().fromJson(new String(data), ShiverGroupImpl.class);
    }
}
