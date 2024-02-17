package com.shiver.components;

import com.shiver.exceptions.*;
import com.shiver.logic.ShiverEventListener;
import com.shiver.logic.ShiverSecurity;
import com.shiver.models.GroupCredentialMessage;
import com.shiver.storage.ShiverDHKeyPairStorage;
import com.shiver.storage.ShiverKeyStorage;
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
class ShiverComponentImpl implements ShiverComponent {
    private final ShiverSecurity shiverSecurity;
    private final ShiverKeyStorage shiverKeyStorage;
    private final ShiverDHKeyPairStorage shiverDHKeyPairStorage;

    ShiverComponentImpl(ShiverSecurity shiverSecurity, ShiverKeyStorage shiverKeyStorage, ShiverDHKeyPairStorage shiverDHKeyPairStorage) {
        this.shiverSecurity = shiverSecurity;
        this.shiverKeyStorage = shiverKeyStorage;
        this.shiverDHKeyPairStorage = shiverDHKeyPairStorage;
    }

    @Override
    public void onStart(ASAPPeer asapPeer) {
        shiverSecurity.onStart(asapPeer);
    }

    @Override
    public void addShiverEventListener(ShiverEventListener shiverEventListener) {
        shiverSecurity.addShiverEventListener(shiverEventListener);
    }

    @Override
    public void removeShiverEventListener(ShiverEventListener shiverEventListener) {
        shiverSecurity.removeShiverEventListener(shiverEventListener);
    }

    @Override
    public CharSequence startCreatingGroupKeyProcess(List<CharSequence> peerIds) throws ShiverDHKeyGenerationException, ShiverGroupSizeException, IOException, ASAPException, ShiverPeerNotVerifiedException {
        return shiverSecurity.startKeyExchangeWithPeers(peerIds);
    }

    @Override
    public void acceptGroupCredentialMessage(GroupCredentialMessage groupCredentialMessage) throws ShiverDHKeyGenerationException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, ASAPException, ShiverPeerNotVerifiedException {
        shiverSecurity.acceptGroupCredentialMessage(groupCredentialMessage);
    }

    @Override
    public boolean hasKeyForGroupId(CharSequence groupId) {
        return shiverKeyStorage.getKeyForGroup(groupId) != null;
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
