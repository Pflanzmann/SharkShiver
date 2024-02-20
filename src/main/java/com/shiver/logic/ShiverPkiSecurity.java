package com.shiver.logic;

import com.shiver.components.ShiverComponent;
import com.shiver.exceptions.ShiverDHKeyGenerationException;
import com.shiver.exceptions.ShiverGroupSizeException;
import com.shiver.exceptions.ShiverPeerNotVerifiedException;
import com.shiver.exceptions.ShiverUnknownURIException;
import com.shiver.models.GroupCredentialMessage;
import com.shiver.models.GroupCredentialMessageImpl;
import com.shiver.models.ShiverPaths;
import com.shiver.storage.ShiverDHKeyPairStorage;
import com.shiver.storage.ShiverKeyStorage;
import net.sharksystem.asap.*;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.utils.Log;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URI;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * This class is the main implementation of the [ShiverSecurity] interface.
 */
public class ShiverPkiSecurity implements ShiverSecurity, ASAPMessageReceivedListener {
    private ASAPPeer asapPeer;
    private final SharkPKIComponent sharkPKIComponent;
    private final ShiverDHKeyPairStorage dhKeyPairStorage;

    private final ShiverKeyStorage shiverKeyStorage;

    private final List<ShiverEventListener> messageReceivers = new ArrayList<>();

    public ShiverPkiSecurity(SharkPKIComponent sharkPKIComponent, ShiverDHKeyPairStorage dhKeyPairStorage, ShiverKeyStorage shiverKeyStorage) {
        this.sharkPKIComponent = sharkPKIComponent;
        this.dhKeyPairStorage = dhKeyPairStorage;
        this.shiverKeyStorage = shiverKeyStorage;
    }

    @Override
    public void onStart(ASAPPeer asapPeer) {
        this.asapPeer = asapPeer;

        asapPeer.addASAPMessageReceivedListener(ShiverComponent.SHARK_SHIVER_APP, this);
    }

    @Override
    public void addShiverEventListener(ShiverEventListener shiverEventListener) {
        this.messageReceivers.add(shiverEventListener);
    }

    @Override
    public void removeShiverEventListener(ShiverEventListener shiverEventListener) {
        this.messageReceivers.remove(shiverEventListener);
    }

    @Override
    public CharSequence startKeyExchangeWithPeers(List<CharSequence> peers) throws ShiverPeerNotVerifiedException, ShiverDHKeyGenerationException, IOException, ASAPException, ShiverGroupSizeException {
        List<CharSequence> orderedListOfPeers = new ArrayList<>(peers);
        orderedListOfPeers.remove(asapPeer.getPeerID());
        orderedListOfPeers.add(0, asapPeer.getPeerID());

        if (orderedListOfPeers.size() <= 1) {
            throw new ShiverGroupSizeException();
        }

        for (CharSequence peer : orderedListOfPeers) {
            if (peer != asapPeer.getPeerID()) {
                if (!verifyPeer(peer)) {
                    throw new ShiverPeerNotVerifiedException();
                }
            }
        }

        CharSequence groupId = UUID.randomUUID().toString();

        KeyPair ownGroupKeyPair = dhKeyPairStorage.getOrGenerateKeyPairForGroup(groupId);

        HashMap<CharSequence, byte[]> keys = new HashMap<>();

        for (CharSequence peer : orderedListOfPeers) {
            if (peer != asapPeer.getPeerID()) {
                keys.put(peer, getPublicKeyBytes(ownGroupKeyPair.getPublic()));
            }
        }

        GroupCredentialMessage groupCredentialMessage = new GroupCredentialMessageImpl(
                groupId,
                orderedListOfPeers,
                keys
        );

        CharSequence receiver = orderedListOfPeers.get(1);
        byte[] messageBytes = groupCredentialMessage.serialize();

        byte[] encryptedMessage = ASAPCryptoAlgorithms.produceEncryptedMessagePackage(messageBytes, receiver, sharkPKIComponent);

        asapPeer.sendASAPMessage(
                ShiverComponent.SHARK_SHIVER_APP,
                ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW.toString(),
                encryptedMessage
        );
        return groupId;
    }

    @Override
    public void acceptGroupCredentialMessage(GroupCredentialMessage groupCredentialMessage) throws IOException, ASAPException, ShiverDHKeyGenerationException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, ShiverPeerNotVerifiedException {
        List<CharSequence> peers = groupCredentialMessage.getPeerIds();
        boolean isLast = peers.indexOf(asapPeer.getPeerID()) == peers.size() - 1;

        KeyPair keyPair = dhKeyPairStorage.getOrGenerateKeyPairForGroup(groupCredentialMessage.getGroupId());
        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
        keyAgreement.init(keyPair.getPrivate());

        if (isLast) {
            Key key = readPublicKeyFromBytes(groupCredentialMessage.getKeys().get(asapPeer.getPeerID()));
            keyAgreement.doPhase(key, true);

            byte[] secret = keyAgreement.generateSecret();
            Key finalKey = new SecretKeySpec(secret, 0, 16, "AES");

            shiverKeyStorage.storeKeyForGroup(groupCredentialMessage.getGroupId(), finalKey);

            for (ShiverEventListener messageReceiver : messageReceivers) {
                messageReceiver.onReceivedGroupKey(groupCredentialMessage.getGroupId());
            }
        } else {
            CharSequence receiver = peers.get(peers.indexOf(asapPeer.getPeerID()) + 1);
            if (verifyPeer(receiver)) {
                throw new ShiverPeerNotVerifiedException();
            }
        }

        for (CharSequence peer : peers) {
            if (peer == asapPeer.getPeerID()) {
                continue;
            }

            byte[] oldKey = groupCredentialMessage.getKeys().get(peer);
            if (oldKey != null) {
                Key key = readPublicKeyFromBytes(oldKey);
                Key newKey = keyAgreement.doPhase(key, false);
                groupCredentialMessage.putKeyForPeerId(peer, getPublicKeyBytes(newKey));
            } else {
                groupCredentialMessage.putKeyForPeerId(peer, getPublicKeyBytes(keyPair.getPublic()));
            }
        }

        if (!isLast) {
            CharSequence receiver = peers.get(peers.indexOf(asapPeer.getPeerID()) + 1);
            byte[] messageBytes = groupCredentialMessage.serialize();

            byte[] encryptedMessage = ASAPCryptoAlgorithms.produceEncryptedMessagePackage(messageBytes, receiver, sharkPKIComponent);

            asapPeer.sendASAPMessage(
                    ShiverComponent.SHARK_SHIVER_APP,
                    ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW.toString(),
                    encryptedMessage
            );
        } else {
            for (CharSequence peer : peers) {
                if (peer == asapPeer.getPeerID()) {
                    continue;
                }

                groupCredentialMessage.getKeys().remove(asapPeer.getPeerID());

                byte[] messageBytes = groupCredentialMessage.serialize();

                byte[] encryptedMessage = ASAPCryptoAlgorithms.produceEncryptedMessagePackage(messageBytes, peer, sharkPKIComponent);

                asapPeer.sendASAPMessage(
                        ShiverComponent.SHARK_SHIVER_APP,
                        ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_BROADCAST.toString(),
                        encryptedMessage
                );
            }
        }
    }

    @Override
    public void asapMessagesReceived(ASAPMessages asapMessages, String s, List<ASAPHop> list) throws IOException {
        URI messageUri = URI.create(asapMessages.getURI().toString());
        ShiverPaths path = ShiverPaths.parsePathByValue(messageUri.getPath());

        if (path == null) {
            Log.writeLogErr(this, "Error receiving message for an unknown path");
            for (ShiverEventListener messageReceiver : messageReceivers) {
                messageReceiver.onErrorReceivingGroupCredentialMessage(asapMessages.getURI().toString(), new ShiverUnknownURIException());
            }

            return;
        }

        switch (path) {
            case SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW -> {
                Iterator<byte[]> messages = asapMessages.getMessages();
                while (messages.hasNext()) {
                    byte[] message = messages.next();

                    try {
                        ASAPCryptoAlgorithms.EncryptedMessagePackage encryptedMessagePackage = ASAPCryptoAlgorithms.parseEncryptedMessagePackage(message);
                        byte[] plainMessageBytes = ASAPCryptoAlgorithms.decryptPackage(encryptedMessagePackage, sharkPKIComponent);

                        GroupCredentialMessage groupCredentialMessage = GroupCredentialMessageImpl.deserialize(plainMessageBytes);

                        for (ShiverEventListener messageReceiver : messageReceivers) {
                            messageReceiver.onReceiveGroupCredentials(groupCredentialMessage);
                        }
                    } catch (Exception e) {
                        Log.writeLogErr(this, "Error receiving groupCredentialMessage and doing the upflow stage", e.getMessage());

                        for (ShiverEventListener messageReceiver : messageReceivers) {
                            messageReceiver.onErrorReceivingGroupCredentialMessage(asapMessages.getURI().toString(), e);
                        }
                    }
                }
            }
            case SHIVER_GROUP_CREDENTIAL_MESSAGE_BROADCAST -> {
                Iterator<byte[]> messages = asapMessages.getMessages();
                while (messages.hasNext()) {
                    byte[] message = messages.next();

                    try {
                        ASAPCryptoAlgorithms.EncryptedMessagePackage encryptedMessagePackage = ASAPCryptoAlgorithms.parseEncryptedMessagePackage(message);
                        byte[] plainMessageBytes = ASAPCryptoAlgorithms.decryptPackage(encryptedMessagePackage, sharkPKIComponent);

                        GroupCredentialMessage groupCredentialMessage = GroupCredentialMessageImpl.deserialize(plainMessageBytes);

                        KeyPair keyPair = dhKeyPairStorage.getOrGenerateKeyPairForGroup(groupCredentialMessage.getGroupId());
                        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
                        keyAgreement.init(keyPair.getPrivate());

                        Key key = readPublicKeyFromBytes(groupCredentialMessage.getKeys().get(asapPeer.getPeerID()));
                        keyAgreement.doPhase(key, true);
                        byte[] secret = keyAgreement.generateSecret();
                        Key finalKey = new SecretKeySpec(secret, 0, 16, "AES");

                        shiverKeyStorage.storeKeyForGroup(groupCredentialMessage.getGroupId(), finalKey);

                        for (ShiverEventListener messageReceiver : messageReceivers) {
                            messageReceiver.onReceivedGroupKey(groupCredentialMessage.getGroupId());
                        }
                    } catch (Exception e) {
                        Log.writeLogErr(this, "Error receiving groupCredentialMessage and doing the broadcast stage", e.getMessage());

                        for (ShiverEventListener messageReceiver : messageReceivers) {
                            messageReceiver.onErrorReceivingGroupCredentialMessage(asapMessages.getURI().toString(), e);
                        }
                    }
                }
            }
        }
    }

    private boolean verifyPeer(CharSequence peerId) throws ShiverPeerNotVerifiedException {
        try {
            return sharkPKIComponent.getCertificateByIssuerAndSubject(sharkPKIComponent.getOwnerID(), peerId) != null;
        } catch (ASAPSecurityException e) {
            throw new ShiverPeerNotVerifiedException(e);
        }
    }

    private byte[] getPublicKeyBytes(Key publicKey) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        String format = publicKey.getFormat();
        String algorithm = publicKey.getAlgorithm();
        byte[] byteEncodedPublicKey = publicKey.getEncoded();

        dos.writeUTF(format);
        dos.writeUTF(algorithm);
        dos.writeInt(byteEncodedPublicKey.length);
        dos.write(byteEncodedPublicKey);

        return baos.toByteArray();
    }

    private PublicKey readPublicKeyFromBytes(byte[] publicKeyBytes) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        ByteArrayInputStream bais = new ByteArrayInputStream(publicKeyBytes);
        DataInputStream dis = new DataInputStream(bais);

        dis.readUTF();
        String algorithm = dis.readUTF();
        int len = dis.readInt();
        byte[] byteEncodedPublicKey = new byte[len];
        dis.readFully(byteEncodedPublicKey);

        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(byteEncodedPublicKey);
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

        return keyFactory.generatePublic(pubKeySpec);
    }
}
