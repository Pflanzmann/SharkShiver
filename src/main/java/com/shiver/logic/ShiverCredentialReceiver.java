package com.shiver.logic;

import com.shiver.models.GroupCredentialMessage;

public interface ShiverCredentialReceiver {
    void receiveGroupCredentials(GroupCredentialMessage groupCredentialMessage);

    void errorVerifyingGroupCredentialMessage(CharSequence groupId, CharSequence senderId);

    void receivedGroupKey(CharSequence groupId);
}
