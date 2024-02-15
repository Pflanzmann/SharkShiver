package com.shiver.components;

import com.shiver.exceptions.*;
import com.shiver.logic.ShiverCredentialReceiver;
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
 * The user should primarily use this interface to manage and manipulate groups, not the underlying classes.
 */
@ASAPFormats(formats = {SharkShiverComponent.SHARK_SHIVER_APP})
public interface SharkShiverComponent extends SharkComponent {
    String SHARK_SHIVER_APP = "SharkShiverApp";

    void addShiverMessageReceiver(ShiverCredentialReceiver shiverMessageReceiver);

    void removeShiverMessageReceiver(ShiverCredentialReceiver shiverMessageReceiver);

    void startCreatingGroupKeyProcess(List<CharSequence> peerIds) throws ShiverDHKeyGenerationException, ShiverGroupSizeException, IOException, ASAPException, ShiverPeerNotVerifiedException;

    void acceptGroupCredentialMessage(GroupCredentialMessage groupCredentialMessage) throws ShiverDHKeyGenerationException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, ASAPException;

    void invalidateGroupKey(CharSequence groupId);

    byte[] encryptMessageForGroup(CharSequence groupId, byte[] message) throws ShiverNoGroupKeyException, ShiverEncryptionException;

    byte[] decryptMessageForGroup(CharSequence groupId, byte[] message) throws ShiverNoGroupKeyException, ShiverDecryptionException;
}
