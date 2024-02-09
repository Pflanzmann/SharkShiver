package com.shiver.components;

import com.google.gson.Gson;
import com.shiver.GroupStorage;
import com.shiver.ShiverSecurity;
import com.shiver.exceptions.ShiverNoGroupException;
import com.shiver.exceptions.ShiverPermissionDeniedException;
import com.shiver.models.*;
import net.sharksystem.SharkException;
import net.sharksystem.asap.*;
import net.sharksystem.asap.utils.ASAPSerialization;
import net.sharksystem.utils.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SharkShiverComponentImpl implements SharkShiverComponent, ASAPEnvironmentChangesListener, ASAPMessageReceivedListener {
    private final GroupStorage groupStorage;
    private final ShiverSecurity shiverSecurity;

    private ASAPPeer ownPeer = null;
    private final HashMap<CharSequence, Membership> ownedMemberships = new HashMap<>();
    private final List<ShiverMessageReceiver> messageReceivers = new ArrayList<>();
    private Set<CharSequence> lastCheckedPeers = new HashSet<>();

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
    public Group createGroup() {
        CharSequence membershipId = ownPeer.getPeerID();
        CharSequence groupId = UUID.randomUUID().toString();

        MembershipImpl ownMember = new MembershipImpl(membershipId, groupId);
        Group group = new GroupImpl(ownMember.getOwnerId(), groupId, new Date());

        groupStorage.storeGroup(group);
        return group;
    }

    @Override
    public void addPeerToGroup(CharSequence groupId, CharSequence peerId) throws ShiverNoGroupException, ASAPException, ShiverPermissionDeniedException, IOException {
        Group group = groupStorage.getGroup(groupId);

        if (!ownedMemberships.containsKey(groupId)) {
            throw new ShiverPermissionDeniedException();
        }

        group.addMember(peerId);

        shiverSecurity.sendSecretToMemberOfGroup(groupId, peerId);

        group.addMember(peerId);
        groupStorage.storeGroup(group);

        publishGroupUpdate(group);
    }

    @Override
    public void removePeerFromGroup(CharSequence groupId, CharSequence peerId) throws ShiverNoGroupException, ASAPException, ShiverPermissionDeniedException, IOException {
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
    public void deleteGroup(CharSequence groupId) throws ASAPException, ShiverNoGroupException, ShiverPermissionDeniedException, IOException {
        Group group = groupStorage.getGroup(groupId);

        if (!ownedMemberships.containsKey(groupId)) {
            throw new ShiverPermissionDeniedException();
        }

        List<CharSequence> memberIds = group.getMemberIdList();
        for (CharSequence groupMember : memberIds) {
            group.removeMember(groupMember);
        }

        publishGroupDelete(group, memberIds);

        groupStorage.deleteGroup(groupId);
        ownedMemberships.remove(groupId);
        shiverSecurity.removeGroupKeys(groupId);
    }

    @Override
    public void sendGroupMessage(CharSequence groupId, byte[] message) throws ShiverNoGroupException, ASAPException, IOException {
        Group group = groupStorage.getGroup(groupId);
        List<CharSequence> membershipIds = group.getMemberIdList();

        for (CharSequence membershipId : membershipIds) {
            byte[] encryptedAndSignedMessage = shiverSecurity.encryptMessageContentForMemberOfGroup(membershipId, groupId, message);
            byte[] preparedMessage = prepareMessage(encryptedAndSignedMessage, ownPeer.getPeerID(), membershipId);

            ownPeer.sendASAPMessage(
                    SharkShiverComponent.SHARK_SHIVER_APP,
                    ShiverPaths.SHIVER_GROUP_UPDATE.getValue(),
                    preparedMessage
            );
        }
    }

    @Override
    public void invalidateMemberForGroup(CharSequence memberId, CharSequence groupId) throws ShiverNoGroupException, ShiverPermissionDeniedException, IOException, ASAPException {
        Group group = groupStorage.getGroup(groupId);

        if (!ownedMemberships.containsKey(groupId)) {
            throw new ShiverPermissionDeniedException();
        }

        List<CharSequence> memberIds = group.getMemberIdList();
        for (CharSequence groupMember : memberIds) {
            group.removeMember(groupMember);
        }

        publishInvalidateMember(group, memberIds, memberId);

        shiverSecurity.invalidateSecretsOfMemberInGroup(memberId, groupId);
    }

    @Override
    public void addShiverMessageReceiver(ShiverMessageReceiver shiverMessageReceiver) {
        this.messageReceivers.add(shiverMessageReceiver);
    }

    @Override
    public void removeShiverMessageReceiver(ShiverMessageReceiver shiverMessageReceiver) {
        this.messageReceivers.remove(shiverMessageReceiver);
    }

    private void publishGroupUpdate(Group group) throws ASAPException, IOException {
        String serializedGroup = new Gson().toJson(group);
        byte[] groupBytes = serializedGroup.getBytes();

        for (CharSequence member : group.getMemberIdList()) {
            if (member == group.getAdminId()) {
                continue;
            }

            byte[] encryptedBytes = shiverSecurity.encryptMessageContentForMemberOfGroup(
                    group.getAdminId(),
                    group.getGroupId(),
                    groupBytes
            );

            String uriQueryParameter = "?group_id=" + group.getGroupId();
            ownPeer.sendASAPMessage(
                    SharkShiverComponent.SHARK_SHIVER_APP,
                    ShiverPaths.SHIVER_GROUP_UPDATE.getValue() + uriQueryParameter,
                    encryptedBytes
            );
        }
    }

    private void publishGroupDelete(Group group, List<CharSequence> members) throws ASAPException, IOException {
        byte[] emptyMessageBytes = new byte[0];

        for (CharSequence member : members) {
            if (member == group.getAdminId()) {
                continue;
            }

            byte[] encryptedBytes = shiverSecurity.encryptMessageContentForMemberOfGroup(
                    group.getAdminId(),
                    group.getGroupId(),
                    emptyMessageBytes
            );

            String uriQueryParameter = "?group_id=" + group.getGroupId();
            ownPeer.sendASAPMessage(
                    SharkShiverComponent.SHARK_SHIVER_APP,
                    ShiverPaths.SHIVER_GROUP_DELETE.getValue() + uriQueryParameter,
                    encryptedBytes
            );
        }
    }

    private void publishInvalidateMember(Group group, List<CharSequence> members, CharSequence memberId) throws ASAPException, IOException {
        byte[] emptyMessageBytes = memberId.toString().getBytes(StandardCharsets.UTF_8);

        for (CharSequence member : members) {
            if (member == group.getAdminId()) {
                continue;
            }

            byte[] encryptedBytes = shiverSecurity.encryptMessageContentForMemberOfGroup(
                    group.getAdminId(),
                    group.getGroupId(),
                    emptyMessageBytes
            );

            String uriQueryParameter = "?group_id=" + group.getGroupId();
            ownPeer.sendASAPMessage(
                    SharkShiverComponent.SHARK_SHIVER_APP,
                    ShiverPaths.SHIVER_INVALIDATE_MEMBER.getValue() + uriQueryParameter,
                    encryptedBytes
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
    public void onlinePeersChanged(Set<CharSequence> set) {
        ArrayList<CharSequence> lastSeenPeersDifference = new ArrayList<>(set);
        lastSeenPeersDifference.removeAll(lastCheckedPeers);

        if (lastSeenPeersDifference.stream().count() == 0) {
            return;
        }

        List<Group> groups = groupStorage.getAllGroups();
        for (CharSequence peerId : lastSeenPeersDifference) {
            for (Group group : groups) {
                try {
                    if (group.getMemberIdList().contains(peerId) && shiverSecurity.isSecretExchangeNeeded(group.getGroupId(), peerId)) {
                        shiverSecurity.sendSecretToMemberOfGroup(group.getGroupId(), peerId);
                    }

                } catch (ASAPException | IOException e) {
                    Log.writeLog(this, "Something went wrong when sending the message");
                }
            }
        }

        lastCheckedPeers = new HashSet<>(lastSeenPeersDifference);
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
                        byte[] plainMessageBytes = shiverSecurity.decryptMessageFromGroup(
                                ownPeer.getPeerID(),
                                groupId,
                                message
                        );

                        for (ShiverMessageReceiver messageReceiver : messageReceivers) {
                            messageReceiver.receiveShiverMessage(groupId, plainMessageBytes);
                        }
                    } catch (Exception e) {
                        Log.writeLog(this, "Could not decrypt and Verify message of member ");
                    }
                }
            }

            case SHIVER_GROUP_UPDATE -> {
                String groupId = messageUri.getQuery().replace("group_id=", "");

                Iterator<byte[]> messages = asapMessages.getMessages();
                while (messages.hasNext()) {
                    byte[] message = messages.next();

                    try {
                        byte[] plainMessageBytes = shiverSecurity.decryptMessageFromGroup(
                                ownPeer.getPeerID(),
                                groupId,
                                message
                        );
                        String plainMessage = new String(plainMessageBytes, StandardCharsets.UTF_8);

                        Group groupUpdate = new Gson().fromJson(plainMessage, GroupImpl.class);
                        groupStorage.storeGroup(groupUpdate);
                    } catch (Exception e) {
                        Log.writeLog(this, "Could not decrypt and verify message of member ");
                    }
                }
            }

            case SHIVER_GROUP_DELETE -> {
                String groupId = messageUri.getQuery().replace("group_id=", "");

                groupStorage.deleteGroup(groupId);
                ownedMemberships.remove(groupId);
                shiverSecurity.removeGroupKeys(groupId);
            }

            case SHIVER_INVALIDATE_MEMBER -> {
                String groupId = messageUri.getQuery().replace("group_id=", "");

                Iterator<byte[]> messages = asapMessages.getMessages();
                while (messages.hasNext()) {
                    byte[] message = messages.next();

                    try {
                        byte[] plainMessageBytes = shiverSecurity.decryptMessageFromGroup(
                                ownPeer.getPeerID(),
                                groupId,
                                message
                        );

                        CharSequence memberId = new String(plainMessageBytes, StandardCharsets.UTF_8);
                        shiverSecurity.invalidateSecretsOfMemberInGroup(memberId, groupId);
                    } catch (Exception e) {
                        Log.writeLog(this, "Could not decrypt, verify or invalidate member");
                    }
                }
            }
        }
    }
}

