package com.shiver;

import com.shiver.models.Group;
import net.sharksystem.asap.ASAPSecurityException;

public interface ShiverSecurity {

    void exchangeSecretWithMemberOfGroup(Group group, CharSequence membershipId) throws ASAPSecurityException;

    byte[] signAndEncryptMessageContentForMemberOfGroup(CharSequence membershipId, CharSequence groupId, byte[] message) throws ASAPSecurityException;

    byte[] decryptAndVerifyMessageFromGroup(CharSequence membershipId, CharSequence groupId, byte[] message) throws ASAPSecurityException;

    boolean removeGroupKeys(String groupId);
}
