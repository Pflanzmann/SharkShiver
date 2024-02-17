package com.shiver.components;

import com.shiver.exceptions.*;
import com.shiver.logic.ShiverEventListener;
import com.shiver.logic.ShiverSecurity;
import com.shiver.models.GroupCredentialMessage;
import com.shiver.storage.ShiverDHKeyPairStorage;
import com.shiver.storage.ShiverKeyStorage;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

public class ShiverComponentImplTest {

    private ShiverComponentImpl shiverComponent;

    private ShiverSecurity mockShiverSecurity;
    private ShiverKeyStorage mockShiverKeyStorage;
    private ShiverDHKeyPairStorage mockShiverDHKeyPairStorage;
    private ASAPPeer mockASAPPeer;

    private CharSequence TEST_GROUP_ID = "groupId";

    @BeforeEach
    public void beforeEach() {
        mockShiverSecurity = Mockito.mock(ShiverSecurity.class);
        mockShiverKeyStorage = Mockito.mock(ShiverKeyStorage.class);
        mockShiverDHKeyPairStorage = Mockito.mock(ShiverDHKeyPairStorage.class);

        shiverComponent = new ShiverComponentImpl(
                mockShiverSecurity,
                mockShiverKeyStorage,
                mockShiverDHKeyPairStorage
        );

        mockASAPPeer = Mockito.mock(ASAPPeer.class);
    }

    @Test
    public void onStart_callsOnStartOfSecurity() {
        shiverComponent.onStart(mockASAPPeer);

        Mockito.verify(mockShiverSecurity, Mockito.times(1)).onStart(mockASAPPeer);
    }

    @Test
    public void addShiverEventListener_delegatesMethodToSecurity() {
        ShiverEventListener mockShiverEventListener = Mockito.mock(ShiverEventListener.class);

        shiverComponent.addShiverEventListener(mockShiverEventListener);

        Mockito.verify(mockShiverSecurity, Mockito.times(1)).addShiverEventListener(mockShiverEventListener);
    }

    @Test
    public void removeShiverEventListener_delegatesMethodToSecurity() {
        ShiverEventListener mockShiverEventListener = Mockito.mock(ShiverEventListener.class);

        shiverComponent.removeShiverEventListener(mockShiverEventListener);

        Mockito.verify(mockShiverSecurity, Mockito.times(1)).removeShiverEventListener(mockShiverEventListener);
    }

    @Test
    public void startCreatingGroupKeyProcess() throws ShiverDHKeyGenerationException, IOException, ASAPException, ShiverGroupSizeException, ShiverPeerNotVerifiedException {
        String testGroupId = "testGroupId";
        List<CharSequence> testPeers = new ArrayList<>();

        Mockito.when(mockShiverSecurity.startKeyExchangeWithPeers(testPeers)).thenReturn(testGroupId);

        CharSequence result = shiverComponent.startCreatingGroupKeyProcess(testPeers);

        Assertions.assertEquals(testGroupId, result);
        Mockito.verify(mockShiverSecurity, Mockito.times(1)).startKeyExchangeWithPeers(testPeers);
    }

    @Test
    public void acceptGroupCredentialMessage_doesNotThrow() throws ShiverDHKeyGenerationException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, ASAPException, ShiverPeerNotVerifiedException {
        GroupCredentialMessage mockGroupCredentialMessage = Mockito.mock(GroupCredentialMessage.class);

        Assertions.assertDoesNotThrow(() -> {
            shiverComponent.acceptGroupCredentialMessage(mockGroupCredentialMessage);
        });

        Mockito.verify(mockShiverSecurity, Mockito.times(1)).acceptGroupCredentialMessage(mockGroupCredentialMessage);
    }

    @Test
    public void acceptGroupCredentialMessage_doesThrow() throws ShiverDHKeyGenerationException, IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, ASAPException, ShiverPeerNotVerifiedException {
        GroupCredentialMessage mockGroupCredentialMessage = Mockito.mock(GroupCredentialMessage.class);

        Mockito.doThrow(new ShiverDHKeyGenerationException()).when(mockShiverSecurity).acceptGroupCredentialMessage(mockGroupCredentialMessage);

        Assertions.assertThrows(Exception.class, () -> {
            shiverComponent.acceptGroupCredentialMessage(mockGroupCredentialMessage);
        });

        Mockito.verify(mockShiverSecurity, Mockito.times(1)).acceptGroupCredentialMessage(mockGroupCredentialMessage);
    }

    @Test
    public void hasKeyForGroupId_returnsTrue() {
        Mockito.when(mockShiverKeyStorage.getKeyForGroup(TEST_GROUP_ID)).thenReturn(Mockito.mock(Key.class));

        boolean result = shiverComponent.hasKeyForGroupId(TEST_GROUP_ID);

        Assertions.assertTrue(result);

        Mockito.verify(mockShiverKeyStorage, Mockito.times(1)).getKeyForGroup(TEST_GROUP_ID);
    }

    @Test
    public void hasKeyForGroupId_returnsFalse() {
        Mockito.when(mockShiverKeyStorage.getKeyForGroup(TEST_GROUP_ID)).thenReturn(null);

        boolean result = shiverComponent.hasKeyForGroupId(TEST_GROUP_ID);

        Assertions.assertFalse(result);

        Mockito.verify(mockShiverKeyStorage, Mockito.times(1)).getKeyForGroup(TEST_GROUP_ID);
    }

    @Test
    public void invalidateGroupKey() {
        shiverComponent.invalidateGroupKey(TEST_GROUP_ID);

        Mockito.verify(mockShiverKeyStorage, Mockito.times(1)).deleteKeyForGroup(TEST_GROUP_ID);
        Mockito.verify(mockShiverDHKeyPairStorage, Mockito.times(1)).deleteKeyPairForGroupId(TEST_GROUP_ID);
    }

    @Test
    public void encryptMessageForGroup_success() throws ShiverNoGroupKeyException, ShiverEncryptionException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        String testMessage = "test_message";

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey testKey = keyGenerator.generateKey();
        Mockito.when(mockShiverKeyStorage.getKeyForGroup(TEST_GROUP_ID)).thenReturn(testKey);

        byte[] result = shiverComponent.encryptMessageForGroup(TEST_GROUP_ID, testMessage.getBytes());
        String encryptedMessageCheck = new String(result);

        Cipher symmetricCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        symmetricCipher.init(2, testKey);
        String decryptedMessageCheck = new String(symmetricCipher.doFinal(result));

        Assertions.assertNotEquals(encryptedMessageCheck, testMessage);
        Assertions.assertEquals(decryptedMessageCheck, testMessage);
    }

    @Test
    public void encryptMessageForGroup_throwsShiverNoGroupKeyException() {
        String testMessage = "test_message";

        byte[] invalidKey = new byte[]{
                (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03,
                (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07,
                (byte) 0x08, (byte) 0x09, (byte) 0x0A, (byte) 0x0B
        };

        SecretKey testKey = new SecretKeySpec(invalidKey, "AES");
        Mockito.when(mockShiverKeyStorage.getKeyForGroup(TEST_GROUP_ID)).thenReturn(testKey);

        Assertions.assertThrows(ShiverEncryptionException.class, () -> {
            shiverComponent.encryptMessageForGroup(TEST_GROUP_ID, testMessage.getBytes());
        });
    }

    @Test
    public void encryptMessageForGroup_noKey() {
        String testMessage = "test_message";

        Mockito.when(mockShiverKeyStorage.getKeyForGroup(TEST_GROUP_ID)).thenReturn(null);

        Assertions.assertThrows(ShiverNoGroupKeyException.class, () -> {
            shiverComponent.encryptMessageForGroup(TEST_GROUP_ID, testMessage.getBytes());
        });
    }

    @Test
    public void decryptMessageForGroup() throws ShiverNoGroupKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, ShiverDecryptionException {
        String testMessage = "test_message";

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey testKey = keyGenerator.generateKey();
        Mockito.when(mockShiverKeyStorage.getKeyForGroup(TEST_GROUP_ID)).thenReturn(testKey);

        Cipher symmetricCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        symmetricCipher.init(1, testKey);
        byte[] testEncryptedMessage = symmetricCipher.doFinal(testMessage.getBytes());

        byte[] result = shiverComponent.decryptMessageForGroup(TEST_GROUP_ID, testEncryptedMessage);
        String decryptedMessageCheck = new String(result);

        Assertions.assertNotEquals(testEncryptedMessage, testMessage);
        Assertions.assertEquals(decryptedMessageCheck, testMessage);
    }

    @Test
    public void decryptMessageForGroup_noKey() {
        String testMessage = "test_message";

        Mockito.when(mockShiverKeyStorage.getKeyForGroup(TEST_GROUP_ID)).thenReturn(null);

        Assertions.assertThrows(ShiverNoGroupKeyException.class, () -> {
            shiverComponent.decryptMessageForGroup(TEST_GROUP_ID, testMessage.getBytes());
        });
    }

    @Test
    public void decryptMessageForGroup_throwsShiverDecryptionException() throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        String testMessage = "test_message";

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey testKey = keyGenerator.generateKey();
        SecretKey invalidKey = keyGenerator.generateKey();

        Mockito.when(mockShiverKeyStorage.getKeyForGroup(TEST_GROUP_ID)).thenReturn(invalidKey);

        Cipher symmetricCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        symmetricCipher.init(1, testKey);
        byte[] testEncryptedMessage = symmetricCipher.doFinal(testMessage.getBytes());

        Assertions.assertThrows(ShiverDecryptionException.class, () -> {
            shiverComponent.decryptMessageForGroup(TEST_GROUP_ID, testEncryptedMessage);
        });
    }
}
