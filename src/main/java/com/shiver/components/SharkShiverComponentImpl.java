package com.shiver.components;

import com.google.gson.Gson;
import com.shiver.GroupStorage;
import com.shiver.ShiverSecurity;
import com.shiver.exceptions.NoGroupAvailableException;
import com.shiver.exceptions.ShiverPermissionDeniedException;
import com.shiver.models.*;
import net.sharksystem.SharkException;
import net.sharksystem.asap.*;
import net.sharksystem.asap.utils.ASAPSerialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SharkShiverComponentImpl implements SharkShiverComponent, ASAPMessageReceivedListener {
    private ASAPPeer ownPeer = null;
    private HashMap<CharSequence, Membership> ownedMemberships = new HashMap<>();
    private final GroupStorage groupStorage;
    private final ShiverSecurity shiverSecurity;

    private List<ShiverMessageReceiver> messageReceivers = new ArrayList<>();

    public SharkShiverComponentImpl(GroupStorage groupStorage, ShiverSecurity shiverSecurity) {
        this.groupStorage = groupStorage;
        this.shiverSecurity = shiverSecurity;
    }

    @Override
    public void onStart(ASAPPeer asapPeer) throws SharkException {
        ownPeer = asapPeer;
        ownPeer.addASAPMessageReceivedListener(SHARK_SHIVER_APP, this);
    }

    @Override
    public Group createGroup() throws ASAPException {
        CharSequence membershipId = ownPeer.getPeerID();
        CharSequence groupId = UUID.randomUUID().toString();

        MembershipImpl ownMember = new MembershipImpl(membershipId, groupId);
        Group group = new GroupImpl(ownMember.getOwnerId(), groupId, new Date());

        groupStorage.storeGroup(group);
        return group;
    }

    @Override
    public void addPeerToGroup(CharSequence groupId, CharSequence peerId) throws NoGroupAvailableException, ASAPException, ShiverPermissionDeniedException, IOException {
        Group group = groupStorage.getGroup(groupId);

        if (!ownedMemberships.containsKey(groupId)) {
            throw new ShiverPermissionDeniedException();
        }

        group.addMember(peerId);

        shiverSecurity.exchangeSecretWithMemberOfGroup(group, peerId);

        group.addMember(peerId);
        groupStorage.storeGroup(group);

        publishGroupUpdate(group);
    }

    @Override
    public void removePeerFromGroup(CharSequence groupId, CharSequence peerId) throws NoGroupAvailableException, ASAPException, ShiverPermissionDeniedException, IOException {
        Group group = groupStorage.getGroup(groupId);

        if (!ownedMemberships.containsKey(groupId)) {
            throw new ShiverPermissionDeniedException();
        }

        group.removeMember(peerId);
        groupStorage.storeGroup(group);

        List<CharSequence> removedMember = new ArrayList<>();
        removedMember.add(peerId);
        publishGroupDelete(group, removedMember);

        publishGroupUpdate(group);
    }

    @Override
    public void deleteGroup(CharSequence groupId) throws ASAPException, NoGroupAvailableException, ShiverPermissionDeniedException, IOException {
        Group group = groupStorage.getGroup(groupId);

        if (!ownedMemberships.containsKey(groupId)) {
            throw new ShiverPermissionDeniedException();
        }

        List<CharSequence> memberIds = group.getMemberIdList();
        for (CharSequence groupMember : memberIds) {
            group.removeMember(groupMember);
        }

        publishGroupDelete(group, memberIds);
    }

    @Override
    public void sendGroupMessage(CharSequence groupId, byte[] message) throws NoGroupAvailableException, ASAPException, IOException {
        Group group = groupStorage.getGroup(groupId);
        List<CharSequence> membershipIds = group.getMemberIdList();

        for (CharSequence membershipId : membershipIds) {
            byte[] encryptedAndSignedMessage = shiverSecurity.signAndEncryptMessageContentForMemberOfGroup(membershipId, groupId, message);
            byte[] preparedMessage = prepareMessage(encryptedAndSignedMessage, ownPeer.getPeerID(), membershipId);

            ownPeer.sendASAPMessage(
                    SharkShiverComponent.SHARK_SHIVER_APP,
                    ShiverPaths.SHIVER_GROUP_UPDATE.getValue(),
                    preparedMessage
            );
        }
    }

    private void publishGroupDelete(Group group, List<CharSequence> members) throws ASAPException, IOException {
        byte[] emptyMessageBytes = new byte[0];

        for (CharSequence member : members) {
            if (member == group.getAdminId()) {
                continue;
            }

            byte[] encryptedBytes = shiverSecurity.signAndEncryptMessageContentForMemberOfGroup(
                    group.getAdminId(),
                    group.getGroupId(),
                    emptyMessageBytes
            );

            byte[] preparedMessageBytes = prepareMessage(
                    encryptedBytes,
                    group.getAdminId(),
                    member
            );

            String uriQueryParameter = "?group_id=" + group.getGroupId();
            ownPeer.sendASAPMessage(
                    SharkShiverComponent.SHARK_SHIVER_APP,
                    ShiverPaths.SHIVER_GROUP_DELETE.getValue() + uriQueryParameter,
                    preparedMessageBytes
            );
        }
    }

    private void publishGroupUpdate(Group group) throws ASAPException, IOException {
        String serializedGroup = new Gson().toJson(group);
        byte[] groupBytes = serializedGroup.getBytes();

        for (CharSequence member : group.getMemberIdList()) {
            if (member == group.getAdminId()) {
                continue;
            }

            byte[] encryptedBytes = shiverSecurity.signAndEncryptMessageContentForMemberOfGroup(
                    group.getAdminId(),
                    group.getGroupId(),
                    groupBytes
            );

            byte[] preparedMessageBytes = prepareMessage(
                    encryptedBytes,
                    group.getAdminId(),
                    member
            );

            String uriQueryParameter = "?group_id=" + group.getGroupId();
            ownPeer.sendASAPMessage(
                    SharkShiverComponent.SHARK_SHIVER_APP,
                    ShiverPaths.SHIVER_GROUP_UPDATE.getValue() + uriQueryParameter,
                    preparedMessageBytes
            );
        }
    }

    private byte[] prepareMessage(byte[] message, CharSequence sender, CharSequence receiver) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ASAPSerialization.writeByteArray(message, outputStream);
        ASAPSerialization.writeCharSequenceParameter(sender, outputStream);
        ASAPSerialization.writeCharSequenceParameter(receiver, outputStream);
        ASAPSerialization.writeLongParameter(System.currentTimeMillis(), outputStream);

        return outputStream.toByteArray();
    }

    @Override
    public void asapMessagesReceived(ASAPMessages asapMessages, String s, List<ASAPHop> list) throws IOException {
        URI messageUri = URI.create(asapMessages.getURI().toString());
        ShiverPaths path = ShiverPaths.parsePathByValue(messageUri.getPath());

        switch (path) {
            case SHIVER_GROUP_Message -> {
                String groupId = messageUri.getQuery().replace("group_id=", "");

                Iterator<byte[]> messages = asapMessages.getMessages();
                while (messages.hasNext()) {
                    byte[] message = messages.next();

                    try {
                        byte[] plainMessageBytes = shiverSecurity.decryptAndVerifyMessageFromGroup(
                                ownPeer.getPeerID(),
                                groupId,
                                message
                        );

                        for (ShiverMessageReceiver messageReceiver : messageReceivers) {
                            messageReceiver.receiveShiverMessage(groupId, plainMessageBytes);
                        }
                    } catch (Exception e) {
                        System.out.println("Could not decrypt and Verify message of member ");
                    }
                }
            }

            case SHIVER_GROUP_UPDATE -> {
                String groupId = messageUri.getQuery().replace("group_id=", "");

                Iterator<byte[]> messages = asapMessages.getMessages();
                while (messages.hasNext()) {
                    byte[] message = messages.next();

                    try {
                        byte[] plainMessageBytes = shiverSecurity.decryptAndVerifyMessageFromGroup(
                                ownPeer.getPeerID(),
                                groupId,
                                message
                        );
                        String plainMessage = new String(plainMessageBytes, StandardCharsets.UTF_8);

                        Group groupUpdate = new Gson().fromJson(plainMessage, GroupImpl.class);
                        groupStorage.storeGroup(groupUpdate);
                    } catch (Exception e) {
                        System.out.println("Could not decrypt and Verify message of member ");
                    }
                }
            }

            case SHIVER_GROUP_DELETE -> {
                String groupId = messageUri.getQuery().replace("group_id=", "");

                groupStorage.deleteGroup(groupId);
                ownedMemberships.remove(groupId);
                shiverSecurity.removeGroupKeys(groupId);
            }
        }
    }
}

