package com.shiver.components;

import com.shiver.exceptions.*;
import com.shiver.logic.ShiverEventListener;
import com.shiver.models.GroupCredentialMessage;
import net.sharksystem.ASAPFormats;
import net.sharksystem.SharkComponent;
import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

/**
 * This interface is the main interface between the user and this library.
 * The user should primarily use this interface to create group keys, decrypt and encrypt messages.
 */
@ASAPFormats(formats = {ShiverComponent.SHARK_SHIVER_APP})
public interface ShiverComponent extends SharkComponent {
    String SHARK_SHIVER_APP = "SharkShiverApp";

    /**
     * Add a receiver for different events from this component
     *
     * @param shiverEventListener
     */
    void addShiverEventListener(ShiverEventListener shiverEventListener);

    /**
     * Remove a receiver for different events from this component
     *
     * @param shiverEventListener
     */
    void removeShiverEventListener(ShiverEventListener shiverEventListener);

    /**
     * Starts the process of creating a group key.
     * When a key is ready to use every event listener gets notified
     *
     * @param peerIds - a list of all peersIds that should be part of the group key.
     *                It does not matter if the owner is in this list or not. This library adds him.
     * @throws ShiverDHKeyGenerationException - Gets thrown if something with the DH-key generation fails
     * @throws ShiverGroupSizeException       - Gets thrown if the group size is 0 or if the only member of the group is the owner
     * @throws IOException
     * @throws ASAPException
     * @throws ShiverPeerNotVerifiedException - Gets thrown if not all peers are verifiable
     */
    void startCreatingGroupKeyProcess(List<CharSequence> peerIds) throws ShiverDHKeyGenerationException, ShiverGroupSizeException, IOException, ASAPException, ShiverPeerNotVerifiedException;

    /**
     * When receiving a {@link GroupCredentialMessage} the end user should accept this manually to prevent unwanted group key creations.
     * This message accepts the groupCredentialMessage and continues the key creation process that someone else started
     * The groupCredentialMessage does get the user from the {@link ShiverEventListener}.
     * The groupCredentialMessage should never get manipulated before accepting it.
     *
     * @param groupCredentialMessage - original groupCredentialMessage that got received via the {@link ShiverEventListener}
     * @throws ShiverDHKeyGenerationException - Gets thrown if something with the DH-key generation fails
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     * @throws ASAPException
     */
    void acceptGroupCredentialMessage(GroupCredentialMessage groupCredentialMessage) throws ShiverDHKeyGenerationException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, ASAPException;

    /**
     * Returns a boolean if a key for a groupId exists or not
     *
     * @param groupId - id of the group
     * @return - does a key exist or not
     */
    boolean hasKeyForGroupId(CharSequence groupId);

    /**
     * Removes all DH keys and group keys known to a group.
     * This should NOT get called after accepting a groupCredentialMessage and awaiting the key
     *
     * @param groupId - id of the group
     */
    void invalidateGroupKey(CharSequence groupId);

    /**
     * Encrypts a message for a given group if the group key exists
     *
     * @param groupId - id of the group
     * @param message - bytes that represent the message
     * @return - the encrypted message
     * @throws ShiverNoGroupKeyException - throws if the key for this group does not exist
     * @throws ShiverEncryptionException - throws if the encryption failed for whatever reason
     */
    byte[] encryptMessageForGroup(CharSequence groupId, byte[] message) throws ShiverNoGroupKeyException, ShiverEncryptionException;

    /**
     * Decrypts a message for a given group if the group key exists
     *
     * @param groupId - id of the group
     * @param message - bytes that represent the encrypted message
     * @return - the decrypted plain message in form of bytes
     * @throws ShiverNoGroupKeyException - throws if the key for this group does not exist
     * @throws ShiverDecryptionException - throws if the decryption failed for whatever reason
     */
    byte[] decryptMessageForGroup(CharSequence groupId, byte[] message) throws ShiverNoGroupKeyException, ShiverDecryptionException;
}
