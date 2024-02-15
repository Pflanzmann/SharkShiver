package com.shiver.logic;

import com.shiver.components.SharkShiverComponent;
import com.shiver.exceptions.ShiverDHKeyGenerationException;
import com.shiver.exceptions.ShiverGroupSizeException;
import com.shiver.exceptions.ShiverPeerNotVerifiedException;
import com.shiver.models.GroupCredentialMessage;
import com.shiver.models.GroupCredentialMessageImpl;
import com.shiver.models.ShiverPaths;
import com.shiver.storager.ShiverDHKeyPairStorage;
import com.shiver.storager.ShiverKeyStorage;
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
 * This class has no need to be loaded or stored because all data is handled by the [SharkPKIComponent] and [GroupStorage]
 */
public class SharkPkiSecurity implements ShiverSecurity, ASAPMessageReceivedListener {
    private ASAPPeer asapPeer;
    private final SharkPKIComponent sharkPKIComponent;
    private final ShiverDHKeyPairStorage dhKeyPairStorage;

    private final ShiverKeyStorage shiverKeyStorage;

    private final List<ShiverCredentialReceiver> messageReceivers = new ArrayList<>();

    public SharkPkiSecurity(ASAPPeer asapPeer, SharkPKIComponent sharkPKIComponent, ShiverDHKeyPairStorage dhKeyPairStorage, ShiverKeyStorage shiverKeyStorage) {
        this.asapPeer = asapPeer;
        this.sharkPKIComponent = sharkPKIComponent;
        this.dhKeyPairStorage = dhKeyPairStorage;
        this.shiverKeyStorage = shiverKeyStorage;
    }

    @Override
    public void onStart(ASAPPeer asapPeer) {
        this.asapPeer = asapPeer;

        asapPeer.addASAPMessageReceivedListener(SharkShiverComponent.SHARK_SHIVER_APP, this);
    }

    @Override
    public void addShiverMessageReceiver(ShiverCredentialReceiver shiverMessageReceiver) {
        this.messageReceivers.add(shiverMessageReceiver);
    }

    @Override
    public void removeShiverMessageReceiver(ShiverCredentialReceiver shiverMessageReceiver) {
        this.messageReceivers.remove(shiverMessageReceiver);
    }

    @Override
    public boolean verifyPeer(CharSequence peerId) throws ShiverPeerNotVerifiedException {
        try {
            return sharkPKIComponent.getCertificateByIssuerAndSubject(sharkPKIComponent.getOwnerID(), peerId) != null;
        } catch (ASAPSecurityException e) {
            throw new ShiverPeerNotVerifiedException(e);
        }
    }

    @Override
    public void startKeyExchangeWithPeers(List<CharSequence> peers) throws ShiverPeerNotVerifiedException, ShiverDHKeyGenerationException, IOException, ASAPException, ShiverGroupSizeException {
        List<CharSequence> orderedListOfPeers = new ArrayList<>(peers);
        orderedListOfPeers.remove(asapPeer.getPeerID());
        orderedListOfPeers.add(0, asapPeer.getPeerID());

        if (peers.size() <= 1) {
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
                SharkShiverComponent.SHARK_SHIVER_APP,
                ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW.toString(),
                encryptedMessage
        );
    }

    @Override
    public void acceptGroupCredentialMessage(GroupCredentialMessage groupCredentialMessage) throws IOException, ASAPException, ShiverDHKeyGenerationException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
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

            for (ShiverCredentialReceiver messageReceiver : messageReceivers) {
                messageReceiver.receivedGroupKey(groupCredentialMessage.getGroupId());
            }
        }

        for (CharSequence peer : peers) {
            if (peer == asapPeer.getPeerID()) {
                continue;
            }

            Key key = readPublicKeyFromBytes(groupCredentialMessage.getKeys().get(peer));
            Key newKey = keyAgreement.doPhase(key, false);
            groupCredentialMessage.putKeyForPeerId(peer, getPublicKeyBytes(newKey));
        }

        if (!isLast) {
            CharSequence receiver = peers.get(peers.indexOf(asapPeer.getPeerID()) + 1);
            byte[] messageBytes = groupCredentialMessage.serialize();

            byte[] encryptedMessage = ASAPCryptoAlgorithms.produceEncryptedMessagePackage(messageBytes, receiver, sharkPKIComponent);

            asapPeer.sendASAPMessage(
                    SharkShiverComponent.SHARK_SHIVER_APP,
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
                        SharkShiverComponent.SHARK_SHIVER_APP,
                        ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW.toString(),
                        encryptedMessage
                );
            }
        }
    }

    @Override
    public void asapMessagesReceived(ASAPMessages asapMessages, String s, List<ASAPHop> list) throws IOException {
        URI messageUri = URI.create(asapMessages.getURI().toString());
        ShiverPaths path = ShiverPaths.parsePathByValue(messageUri.getPath());

        switch (path) {
            case SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW -> {
                Iterator<byte[]> messages = asapMessages.getMessages();
                while (messages.hasNext()) {
                    byte[] message = messages.next();

                    try {
                        byte[] plainMessageBytes = ASAPCryptoAlgorithms.decryptAsymmetric(message, sharkPKIComponent);

                        GroupCredentialMessage groupCredentialMessage = GroupCredentialMessage.deserialize(plainMessageBytes);

                        for (ShiverCredentialReceiver messageReceiver : messageReceivers) {
                            messageReceiver.receiveGroupCredentials(groupCredentialMessage);
                        }
                    } catch (Exception e) {
                        Log.writeLogErr(this, "Could not decrypt GroupCredentialMessage of member", e.getMessage());
                    }
                }
            }
            case SHIVER_GROUP_CREDENTIAL_MESSAGE_BROADCAST -> {
                Iterator<byte[]> messages = asapMessages.getMessages();
                while (messages.hasNext()) {
                    byte[] message = messages.next();

                    try {
                        byte[] plainMessageBytes = ASAPCryptoAlgorithms.decryptAsymmetric(message, sharkPKIComponent);

                        GroupCredentialMessage groupCredentialMessage = GroupCredentialMessage.deserialize(plainMessageBytes);

                        KeyPair keyPair = dhKeyPairStorage.getOrGenerateKeyPairForGroup(groupCredentialMessage.getGroupId());
                        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
                        keyAgreement.init(keyPair.getPrivate());

                        Key key = readPublicKeyFromBytes(groupCredentialMessage.getKeys().get(asapPeer.getPeerID()));
                        keyAgreement.doPhase(key, true);
                        byte[] secret = keyAgreement.generateSecret();
                        Key finalKey = new SecretKeySpec(secret, 0, 16, "AES");

                        shiverKeyStorage.storeKeyForGroup(groupCredentialMessage.getGroupId(), finalKey);

                        for (ShiverCredentialReceiver messageReceiver : messageReceivers) {
                            messageReceiver.receivedGroupKey(groupCredentialMessage.getGroupId());
                        }
                    } catch (Exception e) {
                        Log.writeLogErr(this, "Could not decrypt GroupCredentialMessage of member", e.getMessage());
                    }
                }
            }
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

    public PublicKey readPublicKeyFromBytes(byte[] publicKeyBytes) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
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
