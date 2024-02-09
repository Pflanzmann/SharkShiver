package com.shiver.components;

import com.google.gson.Gson;
import com.shiver.ShiverGroupStorage;
import com.shiver.ShiverSecurity;
import com.shiver.exceptions.ShiverNoGroupException;
import com.shiver.exceptions.ShiverPermissionDeniedException;
import com.shiver.models.ShiverGroup;
import com.shiver.models.ShiverGroupImpl;
import com.shiver.models.ShiverPaths;
import net.sharksystem.asap.*;
import net.sharksystem.asap.utils.ASAPSerialization;
import net.sharksystem.utils.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class is the main implementation of the [SharkShiverComponent] interface.
 * This class does not need to get stored or loaded because of the [GroupStorage] and [ShiverSecurity] interfaces that handle all data
 */
class SharkShiverComponentImpl implements SharkShiverComponent, ASAPEnvironmentChangesListener, ASAPMessageReceivedListener {
    private final ShiverGroupStorage groupStorage;
    private final ShiverSecurity shiverSecurity;

    private ASAPPeer ownPeer = null;
    private final List<ShiverMessageReceiver> messageReceivers = new ArrayList<>();
    private Set<CharSequence> lastCheckedPeers = new HashSet<>();

    public SharkShiverComponentImpl(ShiverSecurity shiverSecurity, ShiverGroupStorage groupStorage) {
        this.shiverSecurity = shiverSecurity;
        this.groupStorage = groupStorage;
    }

    @Override
    public void onStart(ASAPPeer asapPeer) {
        ownPeer = asapPeer;
        ownPeer.addASAPMessageReceivedListener(SHARK_SHIVER_APP, this);
    }

    @Override
    public ShiverGroup createGroup() {
        CharSequence groupId = UUID.randomUUID().toString();

        return groupStorage.createNewGroup(groupId, ownPeer.getPeerID());
    }

    @Override
    public List<ShiverGroup> getAllGroups() {
        return new ArrayList<>(groupStorage.getAllGroups());
    }

    @Override
    public List<CharSequence> getAllMembersOfGroup(CharSequence groupId) throws ShiverNoGroupException {
        return groupStorage.getGroup(groupId).getMemberIdList();
    }

    @Override
    public void addPeerToGroup(CharSequence groupId, CharSequence peerId) throws ShiverNoGroupException, ASAPException, ShiverPermissionDeniedException, IOException {
        ShiverGroup group = groupStorage.getGroup(groupId);

        if (!groupStorage.isAdminOfGroup(groupId, ownPeer.getPeerID())) {
            throw new ShiverPermissionDeniedException();
        }

        group.addMember(peerId);

        shiverSecurity.sendSecretToMemberOfGroup(groupId, peerId);

        group.addMember(peerId);
        groupStorage.storeGroup(group);

        publishGroupUpdate(group);
    }

    @Override
    public void removePeerFromGroup(CharSequence groupId, CharSequence memberId) throws ShiverNoGroupException, ASAPException, ShiverPermissionDeniedException {
        ShiverGroup group = groupStorage.getGroup(groupId);

        if (!groupStorage.isAdminOfGroup(groupId, ownPeer.getPeerID())) {
            throw new ShiverPermissionDeniedException();
        }

        boolean success = group.removeMember(memberId);
        if (!success) {
            Log.writeLog(this, "Removing the member did not work");
            return;
        }

        groupStorage.storeGroup(group);

        List<CharSequence> removedMember = new ArrayList<>();
        removedMember.add(memberId);
        publishGroupDelete(group, removedMember);

        publishGroupUpdate(group);
    }

    @Override
    public void deleteGroup(CharSequence groupId) throws ASAPException, ShiverNoGroupException, ShiverPermissionDeniedException {
        ShiverGroup group = groupStorage.getGroup(groupId);

        if (!groupStorage.isAdminOfGroup(groupId, ownPeer.getPeerID())) {
            throw new ShiverPermissionDeniedException();
        }

        List<CharSequence> memberIds = group.getMemberIdList();
        for (CharSequence groupMember : memberIds) {
            group.removeMember(groupMember);
        }

        publishGroupDelete(group, memberIds);

        groupStorage.deleteGroup(groupId);
        shiverSecurity.removeGroupKeys(groupId);
    }

    @Override
    public void sendGroupMessage(CharSequence groupId, byte[] message) throws ShiverNoGroupException, ASAPException, IOException {
        ShiverGroup group = groupStorage.getGroup(groupId);
        List<CharSequence> membershipIds = group.getMemberIdList();

        for (CharSequence membershipId : membershipIds) {
            if (membershipId != ownPeer.getPeerID()) {
                continue;
            }

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
    public void invalidateMemberForGroup(CharSequence memberId, CharSequence groupId) throws ShiverNoGroupException, ShiverPermissionDeniedException, ASAPException {
        ShiverGroup group = groupStorage.getGroup(groupId);

        if (!groupStorage.isAdminOfGroup(groupId, ownPeer.getPeerID())) {
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
    public void publishGroupUpdate(CharSequence groupId) throws ASAPException, ShiverNoGroupException {
        ShiverGroup group = groupStorage.getGroup(groupId);
        publishGroupUpdate(group);
    }

    @Override
    public void addShiverMessageReceiver(ShiverMessageReceiver shiverMessageReceiver) {
        this.messageReceivers.add(shiverMessageReceiver);
    }

    @Override
    public void removeShiverMessageReceiver(ShiverMessageReceiver shiverMessageReceiver) {
        this.messageReceivers.remove(shiverMessageReceiver);
    }

    private void publishGroupUpdate(ShiverGroup group) throws ASAPException {
        byte[] groupBytes = group.serialize();

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

    private void publishGroupDelete(ShiverGroup group, List<CharSequence> members) throws ASAPException {
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

    private void publishInvalidateMember(ShiverGroup group, List<CharSequence> members, CharSequence memberId) throws ASAPException {
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

        if (lastSeenPeersDifference.isEmpty()) {
            return;
        }

        List<ShiverGroup> groups = groupStorage.getAllGroups();
        for (CharSequence peerId : lastSeenPeersDifference) {
            for (ShiverGroup group : groups) {
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
                        Log.writeLog(this, "Could not decrypt message of member ");
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

                        ShiverGroup groupUpdate = groupStorage.parseGroupFromBytes(plainMessageBytes);

                        try {
                            ShiverGroup group = groupStorage.getGroup(groupId);

                            if (groupUpdate.getGroupIteration() > group.getGroupIteration()) {
                                groupStorage.storeGroup(groupUpdate);
                            }
                            return;
                        } catch (ShiverNoGroupException e) {
                            Log.writeLog(this, "No group in store");
                        }

                        groupStorage.storeGroup(groupUpdate);
                    } catch (ASAPException e) {
                        Log.writeLog(this, "Could not decrypt and verify message of member ");
                    }
                }
            }

            case SHIVER_GROUP_DELETE -> {
                String groupId = messageUri.getQuery().replace("group_id=", "");

                groupStorage.deleteGroup(groupId);
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
