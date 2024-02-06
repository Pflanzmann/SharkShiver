package com.shiver.models;

public class MembershipImpl implements Membership {
    private final CharSequence ownerId;
    private final CharSequence groupId;

    public MembershipImpl(CharSequence ownerId, CharSequence groupId) {
        this.ownerId = ownerId;
        this.groupId = groupId;
    }

    @Override
    public CharSequence getOwnerId() {
        return ownerId;
    }

    @Override
    public CharSequence groupId() {
        return groupId;
    }
}
