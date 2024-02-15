package com.shiver.components;

import com.shiver.exceptions.*;
import com.shiver.logic.ShiverCredentialReceiver;
import com.shiver.logic.ShiverSecurity;
import com.shiver.models.GroupCredentialMessage;
import com.shiver.storager.ShiverDHKeyPairStorage;
import com.shiver.storager.ShiverKeyStorage;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;

import javax.crypto.Cipher;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

/**
 * This class is the main implementation of the [SharkShiverComponent] interface.
 * This class does not need to get stored or loaded because of the [GroupStorage] and [ShiverSecurity] interfaces that handle all data
 * This component implements a single point of truth concept for the group. Only the admin is able to alter the group and
 * members are only able to send messages and exchange certificates.
 */
class SharkShiverComponentImpl implements SharkShiverComponent {
    private final ShiverSecurity shiverSecurity;
    private final ShiverKeyStorage shiverKeyStorage;
    private final ShiverDHKeyPairStorage shiverDHKeyPairStorage;

    SharkShiverComponentImpl(ShiverSecurity shiverSecurity, ShiverKeyStorage shiverKeyStorage, ShiverDHKeyPairStorage shiverDHKeyPairStorage) {
        this.shiverSecurity = shiverSecurity;
        this.shiverKeyStorage = shiverKeyStorage;
        this.shiverDHKeyPairStorage = shiverDHKeyPairStorage;
    }

    @Override
    public void onStart(ASAPPeer asapPeer) {
        shiverSecurity.onStart(asapPeer);
    }

    @Override
    public void addShiverMessageReceiver(ShiverCredentialReceiver shiverMessageReceiver) {
        shiverSecurity.addShiverMessageReceiver(shiverMessageReceiver);
    }

    @Override
    public void removeShiverMessageReceiver(ShiverCredentialReceiver shiverMessageReceiver) {
        shiverSecurity.removeShiverMessageReceiver(shiverMessageReceiver);
    }

    @Override
    public void startCreatingGroupKeyProcess(List<CharSequence> peerIds) throws ShiverDHKeyGenerationException, ShiverGroupSizeException, IOException, ASAPException, ShiverPeerNotVerifiedException {
        shiverSecurity.startKeyExchangeWithPeers(peerIds);
    }

    @Override
    public void acceptGroupCredentialMessage(GroupCredentialMessage groupCredentialMessage) throws ShiverDHKeyGenerationException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, ASAPException {
        shiverSecurity.acceptGroupCredentialMessage(groupCredentialMessage);
    }

    @Override
    public void invalidateGroupKey(CharSequence groupId) {
        shiverKeyStorage.deleteKeyForGroup(groupId);
        shiverDHKeyPairStorage.deleteKeyPairForGroupId(groupId);
    }

    @Override
    public byte[] encryptMessageForGroup(CharSequence groupId, byte[] message) throws ShiverNoGroupKeyException, ShiverEncryptionException {
        Key key = shiverKeyStorage.getKeyForGroup(groupId);
        if (key == null) {
            throw new ShiverNoGroupKeyException();
        }
        try {
            Cipher symmetricCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            symmetricCipher.init(1, key);
            return symmetricCipher.doFinal(message);
        } catch (Exception e) {
            throw new ShiverEncryptionException(e);
        }
    }

    @Override
    public byte[] decryptMessageForGroup(CharSequence groupId, byte[] message) throws ShiverNoGroupKeyException, ShiverDecryptionException {
        Key key = shiverKeyStorage.getKeyForGroup(groupId);
        if (key == null) {
            throw new ShiverNoGroupKeyException();
        }

        try {
            Cipher symmetricCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            symmetricCipher.init(2, key);
            return symmetricCipher.doFinal(message);
        } catch (Exception e) {
            throw new ShiverDecryptionException(e);
        }
    }
}
