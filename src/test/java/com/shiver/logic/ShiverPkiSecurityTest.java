package com.shiver.logic;

import com.shiver.components.ShiverComponent;
import com.shiver.exceptions.ShiverDHKeyGenerationException;
import com.shiver.exceptions.ShiverGroupSizeException;
import com.shiver.exceptions.ShiverPeerNotVerifiedException;
import com.shiver.models.GroupCredentialMessage;
import com.shiver.models.GroupCredentialMessageImpl;
import com.shiver.models.ShiverPaths;
import com.shiver.storage.ShiverDHKeyPairStorage;
import com.shiver.storage.ShiverKeyStorage;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.asap.pki.ASAPCertificate;
import net.sharksystem.pki.SharkPKIComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ShiverPkiSecurityTest {
    private ShiverPkiSecurity shiverPkiSecurity;
    private ShiverKeyStorage mockShiverKeyStorage;
    private ShiverDHKeyPairStorage mockShiverDHKeyPairStorage;
    private SharkPKIComponent mockSharkPKIComponent;
    private ASAPPeer mockASAPPeer;

    private final String TEST_OWN_ASAP_ID = "ownASAPId";
    private final String TEST_PEER_ID_1 = "peerId1";
    private final String TEST_PEER_ID_2 = "peerId2";
    private final CharSequence TEST_GROUP_ID = "groupId";


    @BeforeEach
    public void beforeEach() {
        mockSharkPKIComponent = Mockito.mock(SharkPKIComponent.class);
        mockShiverDHKeyPairStorage = Mockito.mock(ShiverDHKeyPairStorage.class);
        mockShiverKeyStorage = Mockito.mock(ShiverKeyStorage.class);

        shiverPkiSecurity = new ShiverPkiSecurity(
                mockSharkPKIComponent,
                mockShiverDHKeyPairStorage,
                mockShiverKeyStorage
        );

        mockASAPPeer = Mockito.mock(ASAPPeer.class);

        Mockito.when(mockASAPPeer.getPeerID()).thenReturn(TEST_OWN_ASAP_ID);
    }

    @Test
    public void onStart_callsOnStartOfSecurity() {
        shiverPkiSecurity.onStart(mockASAPPeer);

        Mockito.verify(mockASAPPeer, Mockito.times(1)).addASAPMessageReceivedListener(ShiverComponent.SHARK_SHIVER_APP, shiverPkiSecurity);
    }


    @Test
    public void addShiverEventListener() {
        ShiverEventListener mockShiverEventListener = Mockito.mock(ShiverEventListener.class);

        Assertions.assertDoesNotThrow(() -> {
            shiverPkiSecurity.addShiverEventListener(mockShiverEventListener);
        });
    }

    @Test
    public void removeShiverEventListener() {
        ShiverEventListener mockShiverEventListener = Mockito.mock(ShiverEventListener.class);

        Assertions.assertDoesNotThrow(() -> {
            shiverPkiSecurity.removeShiverEventListener(mockShiverEventListener);
        });
    }

    @Test
    public void removeShiverEventListener_afterAdding() {
        ShiverEventListener mockShiverEventListener = Mockito.mock(ShiverEventListener.class);

        shiverPkiSecurity.addShiverEventListener(mockShiverEventListener);

        Assertions.assertDoesNotThrow(() -> {
            shiverPkiSecurity.removeShiverEventListener(mockShiverEventListener);
        });
    }

    @Test
    public void startKeyExchangeWithPeers_successSortsPeers() throws ShiverDHKeyGenerationException, IOException, ShiverGroupSizeException, ASAPException, ShiverPeerNotVerifiedException, NoSuchAlgorithmException, ClassNotFoundException {
        List<CharSequence> testPeers = new ArrayList<>();
        testPeers.add(TEST_PEER_ID_1);
        testPeers.add(TEST_PEER_ID_2);
        testPeers.add(TEST_OWN_ASAP_ID);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(2048);
        KeyPair testKeyPair = keyPairGenerator.generateKeyPair();

        Mockito.when(mockShiverDHKeyPairStorage.getOrGenerateKeyPairForGroup(Mockito.any())).thenReturn(testKeyPair);
        Mockito.when(mockSharkPKIComponent.getCertificateByIssuerAndSubject(Mockito.any(), Mockito.any())).thenReturn(Mockito.mock(ASAPCertificate.class));
        try (MockedStatic<ASAPCryptoAlgorithms> mockedStatic = Mockito.mockStatic(ASAPCryptoAlgorithms.class)) {
            byte[] testArray = new byte[0];

            ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);

            mockedStatic.when(() -> {
                ASAPCryptoAlgorithms.produceEncryptedMessagePackage(messageCaptor.capture(), Mockito.eq(TEST_PEER_ID_1), Mockito.eq(mockSharkPKIComponent));
            }).thenReturn(testArray);

            shiverPkiSecurity.onStart(mockASAPPeer);
            CharSequence result = shiverPkiSecurity.startKeyExchangeWithPeers(testPeers);

            Mockito.verify(mockASAPPeer, Mockito.times(1)).sendASAPMessage(
                    ShiverComponent.SHARK_SHIVER_APP,
                    ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW.toString(),
                    testArray
            );

            mockedStatic.verify(() -> {
                ASAPCryptoAlgorithms.produceEncryptedMessagePackage(Mockito.any(), Mockito.eq(TEST_PEER_ID_1), Mockito.eq(mockSharkPKIComponent));
            }, Mockito.times(1));

            GroupCredentialMessage resultGroupCredential = GroupCredentialMessageImpl.deserialize(messageCaptor.getValue());

            Assertions.assertEquals(resultGroupCredential.getGroupId(), result);

            Assertions.assertEquals(resultGroupCredential.getPeerIds().get(0), TEST_OWN_ASAP_ID);
            Assertions.assertEquals(resultGroupCredential.getPeerIds().get(1), TEST_PEER_ID_1);
            Assertions.assertEquals(resultGroupCredential.getPeerIds().get(2), TEST_PEER_ID_2);

            Assertions.assertNull(resultGroupCredential.getKeys().get(TEST_OWN_ASAP_ID));
            Assertions.assertArrayEquals(getPublicKeyBytes(testKeyPair.getPublic()), resultGroupCredential.getKeys().get(TEST_PEER_ID_1));
            Assertions.assertArrayEquals(getPublicKeyBytes(testKeyPair.getPublic()), resultGroupCredential.getKeys().get(TEST_PEER_ID_2));

            Assertions.assertDoesNotThrow(() -> UUID.fromString(result.toString()));
        }
    }

    @Test
    public void startKeyExchangeWithPeers_throwsShiverGroupSizeException() {
        List<CharSequence> testPeers = new ArrayList<>();
        testPeers.add(TEST_OWN_ASAP_ID);

        shiverPkiSecurity.onStart(mockASAPPeer);
        Assertions.assertThrows(ShiverGroupSizeException.class, () -> {
            shiverPkiSecurity.startKeyExchangeWithPeers(testPeers);
        });
    }

    @Test
    public void acceptGroupCredentialMessage_successNotLast() throws NoSuchAlgorithmException, IOException, ShiverDHKeyGenerationException, InvalidKeySpecException, InvalidKeyException, ASAPException, ClassNotFoundException, ShiverPeerNotVerifiedException {
        List<CharSequence> testPeers = new ArrayList<>();
        testPeers.add(TEST_PEER_ID_1);
        testPeers.add(TEST_OWN_ASAP_ID);
        testPeers.add(TEST_PEER_ID_2);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(2048);
        KeyPair testKeyPair1 = keyPairGenerator.generateKeyPair();
        KeyPair ownKeyPair = keyPairGenerator.generateKeyPair();

        HashMap<CharSequence, byte[]> keys = new HashMap<>();
        keys.put(TEST_PEER_ID_2, getPublicKeyBytes(testKeyPair1.getPublic()));
        keys.put(TEST_OWN_ASAP_ID, getPublicKeyBytes(testKeyPair1.getPublic()));

        KeyAgreement ownTestKeyAgreement = KeyAgreement.getInstance("DH");
        ownTestKeyAgreement.init(ownKeyPair.getPrivate());
        Key resultDHOwnKey = ownTestKeyAgreement.doPhase(testKeyPair1.getPublic(), false);

        GroupCredentialMessage testGroupCredentialMessage = new GroupCredentialMessageImpl(
                TEST_GROUP_ID,
                testPeers,
                keys
        );

        Mockito.when(mockShiverDHKeyPairStorage.getOrGenerateKeyPairForGroup(TEST_GROUP_ID)).thenReturn(ownKeyPair);

        try (MockedStatic<ASAPCryptoAlgorithms> mockedStatic = Mockito.mockStatic(ASAPCryptoAlgorithms.class)) {
            byte[] testArray = new byte[0];
            ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
            mockedStatic.when(() -> {
                ASAPCryptoAlgorithms.produceEncryptedMessagePackage(messageCaptor.capture(), Mockito.eq(TEST_PEER_ID_2), Mockito.eq(mockSharkPKIComponent));
            }).thenReturn(testArray);

            shiverPkiSecurity.onStart(mockASAPPeer);
            shiverPkiSecurity.acceptGroupCredentialMessage(testGroupCredentialMessage);

            mockedStatic.verify(() -> {
                ASAPCryptoAlgorithms.produceEncryptedMessagePackage(Mockito.any(), Mockito.eq(TEST_PEER_ID_2), Mockito.eq(mockSharkPKIComponent));
            }, Mockito.times(1));

            GroupCredentialMessage resultGroupCredential = GroupCredentialMessageImpl.deserialize(messageCaptor.getValue());

            Assertions.assertEquals(resultGroupCredential.getGroupId(), TEST_GROUP_ID);

            Assertions.assertEquals(resultGroupCredential.getPeerIds().get(0), TEST_PEER_ID_1);
            Assertions.assertEquals(resultGroupCredential.getPeerIds().get(1), TEST_OWN_ASAP_ID);
            Assertions.assertEquals(resultGroupCredential.getPeerIds().get(2), TEST_PEER_ID_2);

            Assertions.assertArrayEquals(getPublicKeyBytes(ownKeyPair.getPublic()), resultGroupCredential.getKeys().get(TEST_PEER_ID_1));
            Assertions.assertArrayEquals(getPublicKeyBytes(testKeyPair1.getPublic()), resultGroupCredential.getKeys().get(TEST_OWN_ASAP_ID));
            Assertions.assertArrayEquals(getPublicKeyBytes(resultDHOwnKey), resultGroupCredential.getKeys().get(TEST_PEER_ID_2));

            Mockito.verify(mockASAPPeer).sendASAPMessage(ShiverComponent.SHARK_SHIVER_APP, ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW.toString(), testArray);
        }
    }

    @Test
    public void acceptGroupCredentialMessage_successIsLast_twoMember() throws NoSuchAlgorithmException, IOException, ShiverDHKeyGenerationException, InvalidKeySpecException, InvalidKeyException, ASAPException, ClassNotFoundException, ShiverPeerNotVerifiedException {
        List<CharSequence> testPeers = new ArrayList<>();
        testPeers.add(TEST_PEER_ID_1);
        testPeers.add(TEST_OWN_ASAP_ID);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(2048);
        KeyPair testKeyPair1 = keyPairGenerator.generateKeyPair();
        KeyPair ownKeyPair = keyPairGenerator.generateKeyPair();

        HashMap<CharSequence, byte[]> keys = new HashMap<>();
        keys.put(TEST_OWN_ASAP_ID, getPublicKeyBytes(testKeyPair1.getPublic()));

        KeyAgreement ownTestKeyAgreement = KeyAgreement.getInstance("DH");
        ownTestKeyAgreement.init(ownKeyPair.getPrivate());
        ownTestKeyAgreement.doPhase(testKeyPair1.getPublic(), true);
        byte[] resultOwnSecret = ownTestKeyAgreement.generateSecret();
        Key finalKey = new SecretKeySpec(resultOwnSecret, 0, 16, "AES");

        GroupCredentialMessage testGroupCredentialMessage = new GroupCredentialMessageImpl(
                TEST_GROUP_ID,
                testPeers,
                keys
        );

        Mockito.when(mockShiverDHKeyPairStorage.getOrGenerateKeyPairForGroup(TEST_GROUP_ID)).thenReturn(ownKeyPair);
        Mockito.when(mockSharkPKIComponent.getCertificateByIssuerAndSubject(Mockito.any(), Mockito.any())).thenReturn(Mockito.mock(ASAPCertificate.class));

        try (MockedStatic<ASAPCryptoAlgorithms> mockedStatic = Mockito.mockStatic(ASAPCryptoAlgorithms.class)) {
            byte[] testArray = new byte[0];
            ArgumentCaptor<byte[]> messageCaptor = ArgumentCaptor.forClass(byte[].class);
            mockedStatic.when(() -> {
                ASAPCryptoAlgorithms.produceEncryptedMessagePackage(messageCaptor.capture(), Mockito.eq(TEST_PEER_ID_1), Mockito.eq(mockSharkPKIComponent));
            }).thenReturn(testArray);

            ShiverEventListener mockShiverEventListener = Mockito.mock(ShiverEventListener.class);

            shiverPkiSecurity.onStart(mockASAPPeer);
            shiverPkiSecurity.addShiverEventListener(mockShiverEventListener);
            shiverPkiSecurity.acceptGroupCredentialMessage(testGroupCredentialMessage);

            mockedStatic.verify(() -> {
                ASAPCryptoAlgorithms.produceEncryptedMessagePackage(Mockito.any(), Mockito.eq(TEST_PEER_ID_1), Mockito.eq(mockSharkPKIComponent));
            }, Mockito.times(1));

            GroupCredentialMessage resultGroupCredential = GroupCredentialMessageImpl.deserialize(messageCaptor.getValue());

            Assertions.assertEquals(resultGroupCredential.getGroupId(), TEST_GROUP_ID);

            Assertions.assertEquals(resultGroupCredential.getPeerIds().get(0), TEST_PEER_ID_1);
            Assertions.assertEquals(resultGroupCredential.getPeerIds().get(1), TEST_OWN_ASAP_ID);

            Assertions.assertArrayEquals(getPublicKeyBytes(ownKeyPair.getPublic()), resultGroupCredential.getKeys().get(TEST_PEER_ID_1));
            Assertions.assertArrayEquals(getPublicKeyBytes(testKeyPair1.getPublic()), resultGroupCredential.getKeys().get(TEST_OWN_ASAP_ID));

            Mockito.verify(mockShiverKeyStorage).storeKeyForGroup(TEST_GROUP_ID, finalKey);
            Mockito.verify(mockShiverEventListener).onReceivedGroupKey(TEST_GROUP_ID);
            Mockito.verify(mockASAPPeer).sendASAPMessage(ShiverComponent.SHARK_SHIVER_APP, ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_BROADCAST.getValue(), testArray);
        }
    }

    @Test
    public void asapMessagesReceived_successUpcast() throws ShiverDHKeyGenerationException, IOException, NoSuchAlgorithmException {
        List<CharSequence> testPeers = new ArrayList<>();
        testPeers.add(TEST_PEER_ID_1);
        testPeers.add(TEST_OWN_ASAP_ID);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(2048);
        KeyPair testKeyPair1 = keyPairGenerator.generateKeyPair();
        KeyPair ownKeyPair = keyPairGenerator.generateKeyPair();

        HashMap<CharSequence, byte[]> keys = new HashMap<>();
        keys.put(TEST_OWN_ASAP_ID, getPublicKeyBytes(testKeyPair1.getPublic()));

        GroupCredentialMessage testGroupCredentialMessage = new GroupCredentialMessageImpl(
                TEST_GROUP_ID,
                testPeers,
                keys
        );

        Mockito.when(mockShiverDHKeyPairStorage.getOrGenerateKeyPairForGroup(TEST_GROUP_ID)).thenReturn(ownKeyPair);

        byte[] testArray = "testArray".getBytes();
        ArrayList<byte[]> messages = new ArrayList<>();
        messages.add(testArray);

        ASAPMessages mockASAPMessages = Mockito.mock(ASAPMessages.class);
        Mockito.when(mockASAPMessages.getMessages()).thenReturn(messages.iterator());
        Mockito.when(mockASAPMessages.getURI()).thenReturn(ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW.toString());

        try (MockedStatic<ASAPCryptoAlgorithms> mockedStatic = Mockito.mockStatic(ASAPCryptoAlgorithms.class)) {
            byte[] plainMessageBytes = testGroupCredentialMessage.serialize();

            ASAPCryptoAlgorithms.EncryptedMessagePackage mockEncryptedMessagePackage = Mockito.mock(ASAPCryptoAlgorithms.EncryptedMessagePackage.class);

            mockedStatic.when(() -> {
                ASAPCryptoAlgorithms.parseEncryptedMessagePackage(testArray);
            }).thenReturn(mockEncryptedMessagePackage);

            mockedStatic.when(() -> {
                ASAPCryptoAlgorithms.decryptPackage(mockEncryptedMessagePackage, mockSharkPKIComponent);
            }).thenReturn(plainMessageBytes);

            ShiverEventListener mockShiverEventListener = Mockito.mock(ShiverEventListener.class);

            shiverPkiSecurity.onStart(mockASAPPeer);
            shiverPkiSecurity.addShiverEventListener(mockShiverEventListener);
            shiverPkiSecurity.asapMessagesReceived(mockASAPMessages, TEST_PEER_ID_1, null);

            mockedStatic.verify(() -> {
                ASAPCryptoAlgorithms.parseEncryptedMessagePackage(testArray);
            }, Mockito.times(1));

            mockedStatic.verify(() -> {
                ASAPCryptoAlgorithms.decryptPackage(mockEncryptedMessagePackage, mockSharkPKIComponent);
            }, Mockito.times(1));

            ArgumentCaptor<GroupCredentialMessage> credentialMessageArgumentCaptor = ArgumentCaptor.forClass(GroupCredentialMessage.class);

            Mockito.verify(mockShiverEventListener).onReceiveGroupCredentials(credentialMessageArgumentCaptor.capture());

            GroupCredentialMessage result = credentialMessageArgumentCaptor.getValue();
            Assertions.assertEquals(testGroupCredentialMessage.getGroupId(), result.getGroupId());
            Assertions.assertArrayEquals(testGroupCredentialMessage.getPeerIds().toArray(), result.getPeerIds().toArray());
            Assertions.assertArrayEquals(testGroupCredentialMessage.getKeys().get(TEST_PEER_ID_1), result.getKeys().get(TEST_PEER_ID_1));
            Assertions.assertArrayEquals(testGroupCredentialMessage.getKeys().get(TEST_OWN_ASAP_ID), result.getKeys().get(TEST_OWN_ASAP_ID));
        }
    }

    @Test
    public void asapMessagesReceived_failed() throws IOException {
        byte[] testArray = "testArray".getBytes();
        ArrayList<byte[]> messages = new ArrayList<>();
        messages.add(testArray);

        ASAPMessages mockASAPMessages = Mockito.mock(ASAPMessages.class);
        Mockito.when(mockASAPMessages.getMessages()).thenReturn(messages.iterator());
        Mockito.when(mockASAPMessages.getURI()).thenReturn(ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW.toString());

        try (MockedStatic<ASAPCryptoAlgorithms> mockedStatic = Mockito.mockStatic(ASAPCryptoAlgorithms.class)) {
            byte[] plainMessageBytes = new byte[212];

            ASAPCryptoAlgorithms.EncryptedMessagePackage mockEncryptedMessagePackage = Mockito.mock(ASAPCryptoAlgorithms.EncryptedMessagePackage.class);

            mockedStatic.when(() -> {
                ASAPCryptoAlgorithms.parseEncryptedMessagePackage(testArray);
            }).thenReturn(mockEncryptedMessagePackage);

            mockedStatic.when(() -> {
                ASAPCryptoAlgorithms.decryptPackage(mockEncryptedMessagePackage, mockSharkPKIComponent);
            }).thenReturn(plainMessageBytes);

            ShiverEventListener mockShiverEventListener = Mockito.mock(ShiverEventListener.class);

            shiverPkiSecurity.onStart(mockASAPPeer);
            shiverPkiSecurity.addShiverEventListener(mockShiverEventListener);
            shiverPkiSecurity.asapMessagesReceived(mockASAPMessages, TEST_PEER_ID_1, null);

            mockedStatic.verify(() -> {
                ASAPCryptoAlgorithms.parseEncryptedMessagePackage(testArray);
            }, Mockito.times(1));

            mockedStatic.verify(() -> {
                ASAPCryptoAlgorithms.decryptPackage(mockEncryptedMessagePackage, mockSharkPKIComponent);
            }, Mockito.times(1));

            Mockito.verify(mockShiverEventListener).onErrorReceivingGroupCredentialMessage(Mockito.eq(ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW.toString()), Mockito.any());
        }
    }

    @Test
    public void asapMessagesReceived_failedPath() throws IOException {
        byte[] testArray = "testArray".getBytes();
        ArrayList<byte[]> messages = new ArrayList<>();
        messages.add(testArray);

        ASAPMessages mockASAPMessages = Mockito.mock(ASAPMessages.class);
        Mockito.when(mockASAPMessages.getMessages()).thenReturn(messages.iterator());
        Mockito.when(mockASAPMessages.getURI()).thenReturn("SomeWrongPath");

        try (MockedStatic<ASAPCryptoAlgorithms> mockedStatic = Mockito.mockStatic(ASAPCryptoAlgorithms.class)) {
            byte[] plainMessageBytes = new byte[212];

            ASAPCryptoAlgorithms.EncryptedMessagePackage mockEncryptedMessagePackage = Mockito.mock(ASAPCryptoAlgorithms.EncryptedMessagePackage.class);

            mockedStatic.when(() -> {
                ASAPCryptoAlgorithms.parseEncryptedMessagePackage(testArray);
            }).thenReturn(mockEncryptedMessagePackage);

            mockedStatic.when(() -> {
                ASAPCryptoAlgorithms.decryptPackage(mockEncryptedMessagePackage, mockSharkPKIComponent);
            }).thenReturn(plainMessageBytes);

            ShiverEventListener mockShiverEventListener = Mockito.mock(ShiverEventListener.class);

            shiverPkiSecurity.onStart(mockASAPPeer);
            shiverPkiSecurity.addShiverEventListener(mockShiverEventListener);
            shiverPkiSecurity.asapMessagesReceived(mockASAPMessages, TEST_PEER_ID_1, null);

            Mockito.verify(mockShiverEventListener).onErrorReceivingGroupCredentialMessage(Mockito.eq("SomeWrongPath"), Mockito.any());
        }
    }

    @Test
    public void asapMessagesReceived_successBroadcast() throws ShiverDHKeyGenerationException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        List<CharSequence> testPeers = new ArrayList<>();
        testPeers.add(TEST_PEER_ID_1);
        testPeers.add(TEST_OWN_ASAP_ID);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(2048);
        KeyPair testKeyPair1 = keyPairGenerator.generateKeyPair();
        KeyPair ownKeyPair = keyPairGenerator.generateKeyPair();

        HashMap<CharSequence, byte[]> keys = new HashMap<>();
        keys.put(TEST_OWN_ASAP_ID, getPublicKeyBytes(testKeyPair1.getPublic()));

        KeyAgreement ownTestKeyAgreement = KeyAgreement.getInstance("DH");
        ownTestKeyAgreement.init(ownKeyPair.getPrivate());
        ownTestKeyAgreement.doPhase(testKeyPair1.getPublic(), true);
        byte[] resultOwnSecret = ownTestKeyAgreement.generateSecret();
        Key finalKey = new SecretKeySpec(resultOwnSecret, 0, 16, "AES");

        GroupCredentialMessage testGroupCredentialMessage = new GroupCredentialMessageImpl(
                TEST_GROUP_ID,
                testPeers,
                keys
        );

        Mockito.when(mockShiverDHKeyPairStorage.getOrGenerateKeyPairForGroup(TEST_GROUP_ID)).thenReturn(ownKeyPair);

        byte[] testArray = "testArray".getBytes();
        ArrayList<byte[]> messages = new ArrayList<>();
        messages.add(testArray);

        ASAPMessages mockASAPMessages = Mockito.mock(ASAPMessages.class);
        Mockito.when(mockASAPMessages.getMessages()).thenReturn(messages.iterator());
        Mockito.when(mockASAPMessages.getURI()).thenReturn(ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_BROADCAST.toString());

        try (MockedStatic<ASAPCryptoAlgorithms> mockedStatic = Mockito.mockStatic(ASAPCryptoAlgorithms.class)) {
            byte[] plainMessageBytes = testGroupCredentialMessage.serialize();

            ASAPCryptoAlgorithms.EncryptedMessagePackage mockEncryptedMessagePackage = Mockito.mock(ASAPCryptoAlgorithms.EncryptedMessagePackage.class);

            mockedStatic.when(() -> {
                ASAPCryptoAlgorithms.parseEncryptedMessagePackage(testArray);
            }).thenReturn(mockEncryptedMessagePackage);

            mockedStatic.when(() -> {
                ASAPCryptoAlgorithms.decryptPackage(mockEncryptedMessagePackage, mockSharkPKIComponent);
            }).thenReturn(plainMessageBytes);

            ShiverEventListener mockShiverEventListener = Mockito.mock(ShiverEventListener.class);

            shiverPkiSecurity.onStart(mockASAPPeer);
            shiverPkiSecurity.addShiverEventListener(mockShiverEventListener);
            shiverPkiSecurity.asapMessagesReceived(mockASAPMessages, TEST_PEER_ID_1, null);

            mockedStatic.verify(() -> {
                ASAPCryptoAlgorithms.parseEncryptedMessagePackage(testArray);
            }, Mockito.times(1));

            mockedStatic.verify(() -> {
                ASAPCryptoAlgorithms.decryptPackage(mockEncryptedMessagePackage, mockSharkPKIComponent);
            }, Mockito.times(1));

            Mockito.verify(mockShiverEventListener).onReceivedGroupKey(TEST_GROUP_ID);

            Mockito.verify(mockShiverKeyStorage).storeKeyForGroup(TEST_GROUP_ID, finalKey);
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
}
