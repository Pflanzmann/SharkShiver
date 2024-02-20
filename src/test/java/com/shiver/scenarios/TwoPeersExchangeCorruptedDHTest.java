package com.shiver.scenarios;

import com.shiver.components.ShiverComponent;
import com.shiver.components.ShiverComponentFactory;
import com.shiver.exceptions.*;
import com.shiver.logic.ShiverEventListener;
import com.shiver.logic.ShiverPkiSecurity;
import com.shiver.models.GroupCredentialMessage;
import com.shiver.models.ShiverPaths;
import com.shiver.storage.ShiverDHKeyPairStorageInMemo;
import com.shiver.storage.ShiverKeyStoreInMemo;
import net.sharksystem.SharkException;
import net.sharksystem.asap.ASAPMessages;
import net.sharksystem.asap.ASAPPeer;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.asap.pki.ASAPCertificate;
import net.sharksystem.pki.SharkPKIComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

public class TwoPeersExchangeCorruptedDHTest {
    @Test
    public void twoPeersExchangeTest() throws SharkException, ShiverDHKeyGenerationException, ShiverGroupSizeException, IOException, ShiverPeerNotVerifiedException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, ShiverNoGroupKeyException, ShiverEncryptionException, ShiverDecryptionException {
        // ALICE SETUP
        CharSequence alicePeerId = "alicePeerId";
        ASAPPeer aliceMockPeer = Mockito.mock(ASAPPeer.class);
        Mockito.when(aliceMockPeer.getPeerID()).thenReturn(alicePeerId);

        SharkPKIComponent aliceMockPkiComponent = Mockito.mock(SharkPKIComponent.class);
        Mockito.when(aliceMockPkiComponent.getCertificateByIssuerAndSubject(Mockito.any(), Mockito.any())).thenReturn(Mockito.mock(ASAPCertificate.class));

        ShiverDHKeyPairStorageInMemo aliceShiverDHKeyPairStorageInMemo = new ShiverDHKeyPairStorageInMemo();
        ShiverKeyStoreInMemo aliceShiverKeyStoreInMemo = new ShiverKeyStoreInMemo();
        ShiverPkiSecurity aliceShiverSecurity = new ShiverPkiSecurity(aliceMockPkiComponent, aliceShiverDHKeyPairStorageInMemo, aliceShiverKeyStoreInMemo);

        ShiverComponent aliceShiverComponent = (ShiverComponent) new ShiverComponentFactory(aliceShiverSecurity, aliceShiverDHKeyPairStorageInMemo, aliceShiverKeyStoreInMemo).getComponent();

        // BOB SETUP
        CharSequence bobPeerId = "bobPeerId";
        ASAPPeer bobMockPeer = Mockito.mock(ASAPPeer.class);
        Mockito.when(bobMockPeer.getPeerID()).thenReturn(bobPeerId);

        SharkPKIComponent bobMockPkiComponent = Mockito.mock(SharkPKIComponent.class);
        Mockito.when(bobMockPkiComponent.getCertificateByIssuerAndSubject(Mockito.any(), Mockito.any())).thenReturn(Mockito.mock(ASAPCertificate.class));

        ShiverDHKeyPairStorageInMemo bobShiverDHKeyPairStorageInMemo = new ShiverDHKeyPairStorageInMemo();
        ShiverKeyStoreInMemo bobShiverKeyStoreInMemo = new ShiverKeyStoreInMemo();
        ShiverPkiSecurity bobShiverSecurity = new ShiverPkiSecurity(bobMockPkiComponent, bobShiverDHKeyPairStorageInMemo, bobShiverKeyStoreInMemo);

        ShiverComponent bobShiverComponent = (ShiverComponent) new ShiverComponentFactory(bobShiverSecurity, bobShiverDHKeyPairStorageInMemo, bobShiverKeyStoreInMemo).getComponent();

        // MOCK ASAP CRYPTO
        try (MockedStatic<ASAPCryptoAlgorithms> mockASAPCryptoAlgorithms = Mockito.mockStatic(ASAPCryptoAlgorithms.class)) {
            byte[] aliceEncryptedMessage = "aliceMessage".getBytes();

            mockASAPCryptoAlgorithms.when(() -> ASAPCryptoAlgorithms.produceEncryptedMessagePackage(Mockito.any(), Mockito.eq(bobPeerId), Mockito.eq(aliceMockPkiComponent))).thenReturn(aliceEncryptedMessage);

            // VERIFY
            // ALICE START
            List<CharSequence> groupPeers = new ArrayList<>();
            groupPeers.add(bobPeerId);

            aliceShiverComponent.onStart(aliceMockPeer);
            CharSequence aliceCreatedGroupId = aliceShiverComponent.startCreatingGroupKeyProcess(groupPeers);

            // GET ALICE SEND MESSAGE
            ArgumentCaptor<byte[]> aliceMessageArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
            mockASAPCryptoAlgorithms.verify(() -> {
                ASAPCryptoAlgorithms.produceEncryptedMessagePackage(aliceMessageArgumentCaptor.capture(), Mockito.eq(bobPeerId), Mockito.eq(aliceMockPkiComponent));
            }, Mockito.times(1));

            byte[] aliceRealMessage = aliceMessageArgumentCaptor.getValue();

            // BOB RECEIVE UPFLOW MESSAGE
            byte[] bobEncryptedMessage = "bobTestMessage".getBytes();
            ArrayList<byte[]> bobMessages = new ArrayList<>();
            bobMessages.add(bobEncryptedMessage);

            ASAPMessages bobMockASAPMessages = Mockito.mock(ASAPMessages.class);
            Mockito.when(bobMockASAPMessages.getMessages()).thenReturn(bobMessages.iterator());
            Mockito.when(bobMockASAPMessages.getURI()).thenReturn(ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW.toString());

            ASAPCryptoAlgorithms.EncryptedMessagePackage bobMockEncryptedMessagePackage = Mockito.mock(ASAPCryptoAlgorithms.EncryptedMessagePackage.class);
            mockASAPCryptoAlgorithms.when(() -> ASAPCryptoAlgorithms.parseEncryptedMessagePackage(bobEncryptedMessage)).thenReturn(bobMockEncryptedMessagePackage);
            mockASAPCryptoAlgorithms.when(() -> ASAPCryptoAlgorithms.decryptPackage(bobMockEncryptedMessagePackage, bobMockPkiComponent)).thenReturn(aliceRealMessage);
            mockASAPCryptoAlgorithms.when(() -> ASAPCryptoAlgorithms.produceEncryptedMessagePackage(Mockito.any(), Mockito.eq(alicePeerId), Mockito.eq(bobMockPkiComponent))).thenReturn(bobEncryptedMessage);

            List<GroupCredentialMessage> bobReceivedGroupCredentialMessages = new ArrayList<>();
            List<CharSequence> bobReceivedGroupIdForKey = new ArrayList<>();
            ShiverEventListener bobEventListener = new ShiverEventListener() {
                @Override
                public void onReceiveGroupCredentials(GroupCredentialMessage groupCredentialMessage) {
                    //DELETE ALICE DH KEY TO FAIL THE DH
                    aliceShiverDHKeyPairStorageInMemo.deleteKeyPairForGroupId(aliceCreatedGroupId);

                    bobReceivedGroupCredentialMessages.add(groupCredentialMessage);
                }

                @Override
                public void onErrorReceivingGroupCredentialMessage(String path, Exception exception) {
                }

                @Override
                public void onReceivedGroupKey(CharSequence groupId) {
                    bobReceivedGroupIdForKey.add(groupId);
                }
            };

            bobShiverComponent.addShiverEventListener(bobEventListener);

            bobShiverComponent.onStart(bobMockPeer);
            bobShiverSecurity.asapMessagesReceived(bobMockASAPMessages, alicePeerId.toString(), null);

            // VERIFY GROUP CREDENTIALAl MESSAGE
            GroupCredentialMessage bobGroupCredentialMessage = bobReceivedGroupCredentialMessages.get(0);
            Assertions.assertNotNull(bobGroupCredentialMessage);
            bobShiverComponent.acceptGroupCredentialMessage(bobGroupCredentialMessage);

            // GET BOB SEND MESSAGE
            ArgumentCaptor<byte[]> bobMessageArgumentCaptor = ArgumentCaptor.forClass(byte[].class);
            mockASAPCryptoAlgorithms.verify(() -> {
                ASAPCryptoAlgorithms.produceEncryptedMessagePackage(bobMessageArgumentCaptor.capture(), Mockito.eq(alicePeerId), Mockito.eq(bobMockPkiComponent));
            }, Mockito.times(1));

            byte[] bobRealMessage = bobMessageArgumentCaptor.getValue();

            // ALICE RECEIVE BROADCAST MESSAGE
            byte[] aliceTestMessage = "aliceTestMessage".getBytes();
            ArrayList<byte[]> aliceMessages = new ArrayList<>();
            aliceMessages.add(aliceTestMessage);

            ASAPMessages aliceMockASAPMessages = Mockito.mock(ASAPMessages.class);
            Mockito.when(aliceMockASAPMessages.getMessages()).thenReturn(aliceMessages.iterator());
            Mockito.when(aliceMockASAPMessages.getURI()).thenReturn(ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_BROADCAST.toString());

            ASAPCryptoAlgorithms.EncryptedMessagePackage aliceMockEncryptedMessagePackage = Mockito.mock(ASAPCryptoAlgorithms.EncryptedMessagePackage.class);
            mockASAPCryptoAlgorithms.when(() -> ASAPCryptoAlgorithms.parseEncryptedMessagePackage(aliceTestMessage)).thenReturn(aliceMockEncryptedMessagePackage);
            mockASAPCryptoAlgorithms.when(() -> ASAPCryptoAlgorithms.decryptPackage(aliceMockEncryptedMessagePackage, aliceMockPkiComponent)).thenReturn(bobRealMessage);

            aliceShiverSecurity.asapMessagesReceived(aliceMockASAPMessages, bobPeerId.toString(), null);

            // CHECK ENCRYPTION AND DECRYPTION ALICE -> BOB
            // DECRYPTION SHOULD FAIL
            String aliceTestMessageToEncryptString = "Hey this is a test from alice";
            byte[] aliceEncryptedTestMessage = aliceShiverComponent.encryptMessageForGroup(aliceCreatedGroupId, aliceTestMessageToEncryptString.getBytes());

            CharSequence bobGroupId = bobReceivedGroupIdForKey.get(0);
            Assertions.assertThrows(ShiverDecryptionException.class, () -> bobShiverComponent.decryptMessageForGroup(bobGroupId, aliceEncryptedTestMessage));

            // CHECK ENCRYPTION AND DECRYPTION BOB -> ALICE
            // DECRYPTION SHOULD FAIL
            String bobTestMessageToEncryptString = "Hey this is a test from bob";
            byte[] bobEncryptedTestMessage = bobShiverComponent.encryptMessageForGroup(bobGroupId, bobTestMessageToEncryptString.getBytes());

            Assertions.assertThrows(ShiverDecryptionException.class, () -> aliceShiverComponent.decryptMessageForGroup(aliceCreatedGroupId, bobEncryptedTestMessage));
        }
    }
}
