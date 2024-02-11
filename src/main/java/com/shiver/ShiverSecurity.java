package com.shiver;

import com.shiver.exceptions.ShiverDecryptionFailedException;
import com.shiver.exceptions.ShiverEncryptionFailedException;
import com.shiver.exceptions.ShiverSendingSecretFailedException;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;

import java.io.IOException;

/**
 * This interface represents the security aspect of the SharkShiver library and is mainly used by SharkShiverComponentImpl.
 * This enables an easy way to let the developer decide to implement their own logic to handle secrets and decrypt/ encrypt messages.
 * It is mainly used for the ASAP framework, hence the use of ASAP exceptions.
 */
public interface ShiverSecurity {

    /**
     * checks the member and group he is associated with to give information if a secret exchange is necessary or not
     *
     * @param groupId  - the id the member is associated with
     * @param memberId - the id of the member
     * @return - true if a secret exchange is necessary
     */
    boolean isSecretExchangeNeeded(CharSequence groupId, CharSequence memberId);

    /**
     * Accepts the group invite for the group with the associated id from the associated member
     *
     * @param groupId
     * @param memberId
     * @return
     */
    void acceptGroupInvite(CharSequence groupId, CharSequence memberId);

    /**
     * Sends a secret to the member and associates it with the group.
     * This should be called if isSecretExchangeNeeded(...) is returning true
     *
     * @param groupId  - the id the member is associated with
     * @param memberId - the id of the member
     * @throws ASAPException - throws when something went wrong in the underlying ASAP structure
     * @throws IOException   - throws when something went wrong in the underlying ASAP structure
     */
    void sendSecretToMemberOfGroup(CharSequence groupId, CharSequence memberId) throws ShiverSendingSecretFailedException;

    /**
     * encrypts the message and prepares it for sending
     *
     * @param groupId   - the id the member is associated with
     * @param recipient - the id of the member
     * @param message   - The data that should get encrypted
     * @return - the done encrypted message
     * @throws ASAPSecurityException - throws when something went wrong in the underlying ASAP structure
     */
    byte[] encryptMessageContentForMemberOfGroup(CharSequence recipient, CharSequence groupId, byte[] message) throws ShiverEncryptionFailedException;

    /**
     * Decrypts the message with the given senderId and groupId to find the fitting key
     *
     * @param senderId - the id of the sender
     * @param groupId  - the id the sender is associated with
     * @param message  - The encrypted message
     * @return - the raw message that should get delivered
     * @throws ASAPException - throws when something went wrong in the underlying ASAP structure
     * @throws IOException   - throws when something went wrong in the underlying ASAP structure
     */
    byte[] decryptMessageFromGroup(CharSequence senderId, CharSequence groupId, byte[] message) throws ShiverDecryptionFailedException;

    /**
     * Invalidates all keys of a user when his keys got compromised.
     *
     * @param memberId - the id of the member that the invalidation is associated with
     * @param groupId  - the id of the group that the invalidation is associated with
     */
    void invalidateSecretsOfMemberInGroup(CharSequence memberId, CharSequence groupId);

    /**
     * Removes all information of a group and deletes the keys.
     *
     * @param groupId - The id of the group that should get removed
     */
    void removeGroupKeys(CharSequence groupId);
}
