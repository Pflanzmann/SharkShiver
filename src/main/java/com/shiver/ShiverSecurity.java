package com.shiver;

import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;

import java.io.IOException;

public interface ShiverSecurity {

    /**
     * checks the member and group he is associated with to give information if a secret exchange is necessary or not
     *
     * @param groupId  - the id the member is associated with
     * @param memberId - the id of the member
     * @return - true if a secret exchange is necessary
     * @throws ASAPException - throws when something went wrong in the underlying ASAP structure
     */
    boolean isSecretExchangeNeeded(CharSequence groupId, CharSequence memberId) throws ASAPSecurityException;

    /**
     * Sends a secret to the member and associates it with the group.
     * This should be called if isSecretExchangeNeeded(...) is returning true
     *
     * @param groupId  - the id the member is associated with
     * @param memberId - the id of the member
     * @throws ASAPException - throws when something went wrong in the underlying ASAP structure
     * @throws IOException   - throws when something went wrong in the underlying ASAP structure
     */
    void sendSecretToMemberOfGroup(CharSequence groupId, CharSequence memberId) throws ASAPException, IOException;

    /**
     * encrypts the message and prepares it for sending
     *
     * @param groupId  - the id the member is associated with
     * @param memberId - the id of the member
     * @param message  - The data that should get encrypted
     * @return - the done encrypted message
     * @throws ASAPException                     - throws when something went wrong in the underlying ASAP structure
     */
    byte[] encryptMessageContentForMemberOfGroup(CharSequence memberId, CharSequence groupId, byte[] message) throws ASAPSecurityException;

    /**
     * Decrypts the message with the given senderId and groupId to find the fitting key
     *
     * @param senderId - the id of the sender
     * @param groupId  - the id the sender is associated with
     * @param message  - The encrypted message
     * @return - the raw message that should get delivered
     * @throws ASAPException                     - throws when something went wrong in the underlying ASAP structure
     * @throws ASAPException                     - throws when something went wrong in the underlying ASAP structure
     */
    byte[] decryptMessageFromGroup(CharSequence senderId, CharSequence groupId, byte[] message) throws ASAPException, IOException;

    /**
     * Invalidates all keys of a user when his keys got compromised.
     *
     * @param memberId - the id of the member that the invalidation is associated with
     * @param groupId - the id of the group that the invalidation is associated with
     */
    void invalidateSecretsOfMemberInGroup(CharSequence memberId, CharSequence groupId);

    /**
     * Removes all information of a group and deletes the keys.
     *
     * @param groupId - The id of the group that should get removed
     */
    void removeGroupKeys(CharSequence groupId);
}
