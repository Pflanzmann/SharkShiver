package com.shiver.components;

import com.google.gson.Gson;
import com.shiver.SingleGroupStorage;
import com.shiver.exceptions.NoGroupAvailableException;
import com.shiver.models.Group;
import com.shiver.models.GroupImpl;
import com.shiver.models.GroupMember;
import com.shiver.models.ShiverPaths;
import net.sharksystem.SharkException;
import net.sharksystem.asap.*;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.pki.SharkPKIComponent;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class SharkShiverComponentImpl implements SharkShiverComponent, ASAPMessageReceivedListener {
    private ASAPPeer ownPeer = null;
    private GroupMember ownMember = null;
    private SharkPKIComponent sharkPKIComponent = null;
    private SingleGroupStorage singleGroupStorage = null;

    public SharkShiverComponentImpl(SharkPKIComponent sharkPKIComponent, SingleGroupStorage singleGroupStorage) {
        this.sharkPKIComponent = sharkPKIComponent;
        this.singleGroupStorage = singleGroupStorage;
    }

    @Override
    public void onStart(ASAPPeer asapPeer) throws SharkException {
        ownPeer = asapPeer;
    }

    @Override
    public Group createGroup() throws ASAPException {
        Group group = new GroupImpl(ownMember, new Date());
        singleGroupStorage.storeGroup(group);
        return group;
    }

    @Override
    public void addPeerToGroup(GroupMember groupMember) throws NoGroupAvailableException, ASAPException, IOException {
        Group group = singleGroupStorage.getGroup();

        group.addMember(groupMember);

        // Do Key exchange
//        ownPeer.sendASAPMessage();

        group.addMember(groupMember);
        singleGroupStorage.storeGroup(group);

        publishGroupUpdate(group);
    }

    @Override
    public void removePeerFromGroup(GroupMember groupMember) throws NoGroupAvailableException {
        Group group = singleGroupStorage.getGroup();

    }

    @Override
    public void sendGroupMessage(ASAPMessages asapMessages) throws NoGroupAvailableException, IOException, ASAPException {
        Group group = singleGroupStorage.getGroup();

        for (GroupMember member : group.getMemberList()) {
            Iterator<byte[]> messages = asapMessages.getMessages();
            while (messages.hasNext()) {
                byte[] signedMessage = ASAPCryptoAlgorithms.sign(messages.next(), sharkPKIComponent);
                byte[] encryptedBytes = ASAPCryptoAlgorithms.produceEncryptedMessagePackage(signedMessage, member.getMemberId(), sharkPKIComponent);

                ownPeer.sendASAPMessage(
                        SharkShiverComponent.SHARK_SHIVER_APP,
                        ShiverPaths.SHIVER_GROUP_UPDATE_URI.getValue(),
                        encryptedBytes
                );
            }
        }
    }

    @Override
    public void deleteGroup() throws ASAPException, NoGroupAvailableException {
        Group group = singleGroupStorage.getGroup();


    }

    private void publishGroupUpdate(Group group) throws ASAPException, IOException {
        String serializedGroup = new Gson().toJson(group);
        byte[] groupBytes = serializedGroup.getBytes();
        byte[] signedMessage = ASAPCryptoAlgorithms.sign(groupBytes, sharkPKIComponent);

        for (GroupMember member : group.getMemberList()) {
            byte[] encryptedBytes = ASAPCryptoAlgorithms.produceEncryptedMessagePackage(signedMessage, member.getMemberId(), sharkPKIComponent);

            ownPeer.sendASAPMessage(
                    SharkShiverComponent.SHARK_SHIVER_APP,
                    ShiverPaths.SHIVER_GROUP_UPDATE_URI.getValue(),
                    encryptedBytes
            );
        }
    }

    @Override
    public void asapMessagesReceived(ASAPMessages asapMessages, String s, List<ASAPHop> list) throws IOException {
    }
}
