package com.shiver;

import com.shiver.exceptions.ShiverMissingCredentialsException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;

import java.io.IOException;

public interface ShiverSecurity {

    boolean isSecretExchangeNeeded(CharSequence groupId, CharSequence peerId) throws ASAPSecurityException;

    void sendSecretToMemberOfGroup(CharSequence groupId, CharSequence memberId) throws ASAPException, IOException;

    byte[] encryptMessageContentForMemberOfGroup(CharSequence memberId, CharSequence groupId, byte[] message) throws ASAPSecurityException, ShiverMissingCredentialsException;

    byte[] decryptMessageFromGroup(CharSequence memberId, CharSequence groupId, byte[] message) throws ASAPException, ShiverMissingCredentialsException, IOException;

    void removeGroupKeys(String groupId);
}
