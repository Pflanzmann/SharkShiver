package com.shiver.components;

import com.shiver.ShiverGroupFactory;
import com.shiver.ShiverGroupStorage;
import com.shiver.ShiverMediator;
import com.shiver.ShiverSecurity;
import com.shiver.exceptions.ShiverNotSyncableException;
import com.shiver.exceptions.ShiverPermissionDeniedException;
import com.shiver.models.ShiverGroup;
import com.shiver.models.ShiverPaths;
import net.sharksystem.asap.*;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class is the main implementation of the [SharkShiverComponent] interface.
 * This class does not need to get stored or loaded because of the [GroupStorage] and [ShiverSecurity] interfaces that handle all data
 * This component implements a single point of truth concept for the group. Only the admin is able to alter the group and
 * members are only able to send messages and exchange certificates.
 */
class SharkShiverComponentImpl implements SharkShiverComponent, ASAPEnvironmentChangesListener, ASAPMessageReceivedListener {
    private final ShiverGroupStorage groupStorage;
    private final ShiverSecurity shiverSecurity;
    private final ShiverGroupFactory groupFactory;
    private final ShiverMediator shiverMediator;

    private ASAPPeer ownPeer = null;
    private final List<ShiverMessageReceiver> messageReceivers = new ArrayList<>();
    private Set<CharSequence> lastCheckedPeers = new HashSet<>();

    public SharkShiverComponentImpl(ShiverSecurity shiverSecurity, ShiverGroupStorage groupStorage, ShiverGroupFactory groupFactory, ShiverMediator shiverMediator) {
        this.shiverSecurity = shiverSecurity;
        this.groupStorage = groupStorage;
        this.groupFactory = groupFactory;
        this.shiverMediator = shiverMediator;
    }

    @Override
    public void onStart(ASAPPeer asapPeer) {
        ownPeer = asapPeer;
        ownPeer.addASAPMessageReceivedListener(SHARK_SHIVER_APP, this);
    }

    @Override
    public ShiverGroup createGroup() {
        CharSequence groupId = UUID.randomUUID().toString();
        ShiverGroup group = groupFactory.createNewGroup(groupId, ownPeer.getPeerID());
        groupStorage.storeGroup(group);
        return group;
    }

    @Override
    public List<ShiverGroup> getAllGroups() {
        return new ArrayList<>(groupStorage.getAllGroups());
    }

    @Override
    public List<CharSequence> getAllMembersOfGroup(CharSequence groupId) {
        return groupStorage.getGroup(groupId).getMemberIdList();
    }

    @Override
    public void addPeerToGroup(CharSequence groupId, CharSequence peerId) throws ASAPException, ShiverPermissionDeniedException, IOException {
        ShiverGroup group = groupStorage.getGroup(groupId);

        if (!groupStorage.isAdminOfGroup(groupId, ownPeer.getPeerID())) {
            throw new ShiverPermissionDeniedException();
        }

        group.addMember(peerId);

        shiverSecurity.sendSecretToMemberOfGroup(groupId, peerId);

        group.addMember(peerId);
        groupStorage.storeGroup(group);

        publishGroupUpdate(group, false);
    }

    @Override
    public void acceptGroupInvitationForGroupFromPeer(CharSequence groupId, CharSequence peerId) {
        shiverSecurity.acceptGroupInvite(groupId, peerId);
    }

    @Override
    public void removePeerFromGroup(CharSequence groupId, CharSequence memberId) throws ASAPException, ShiverPermissionDeniedException, IOException {
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

        publishGroupUpdate(group, false);
    }

    @Override
    public void deleteGroup(CharSequence groupId) throws ASAPException, ShiverPermissionDeniedException {
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
    public void sendGroupMessage(CharSequence groupId, byte[] message) throws ASAPException {
        ShiverGroup group = groupStorage.getGroup(groupId);
        List<CharSequence> membershipIds = group.getMemberIdList();

        for (CharSequence membershipId : membershipIds) {
            if (membershipId != ownPeer.getPeerID()) {
                continue;
            }

            byte[] encryptedMessage = shiverSecurity.encryptMessageContentForMemberOfGroup(membershipId, groupId, message);

            ownPeer.sendASAPMessage(
                    SharkShiverComponent.SHARK_SHIVER_APP,
                    ShiverPaths.SHIVER_GROUP_UPDATE.getValue(),
                    encryptedMessage
            );
        }
    }

    @Override
    public void invalidateMemberForGroup(CharSequence memberId, CharSequence groupId) throws ShiverPermissionDeniedException, ASAPException {
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
    public void publishGroupUpdate(CharSequence groupId) throws ASAPException, IOException {
        ShiverGroup group = groupStorage.getGroup(groupId);
        publishGroupUpdate(group, false);
    }

    @Override
    public void publishOnlineOnlyGroupUpdate(CharSequence groupId) throws ASAPException, IOException {
        ShiverGroup group = groupStorage.getGroup(groupId);
        publishGroupUpdate(group, true);
    }

    @Override
    public void addShiverMessageReceiver(ShiverMessageReceiver shiverMessageReceiver) {
        this.messageReceivers.add(shiverMessageReceiver);
    }

    @Override
    public void removeShiverMessageReceiver(ShiverMessageReceiver shiverMessageReceiver) {
        this.messageReceivers.remove(shiverMessageReceiver);
    }

    private void publishGroupUpdate(ShiverGroup group, boolean onlineOnly) throws ASAPException, IOException {
        byte[] groupBytes = group.serialize();

        for (CharSequence member : group.getMemberIdList()) {
            if (member == ownPeer.getPeerID()) {
                continue;
            }

            byte[] encryptedBytes = shiverSecurity.encryptMessageContentForMemberOfGroup(
                    member,
                    group.getGroupId(),
                    groupBytes
            );

            String uriQueryParameter = "?group_id=" + group.getGroupId();
            if (onlineOnly) {
                ownPeer.sendTransientASAPMessage(
                        SharkShiverComponent.SHARK_SHIVER_APP,
                        ShiverPaths.SHIVER_GROUP_UPDATE.getValue() + uriQueryParameter,
                        encryptedBytes
                );
            } else {
                ownPeer.sendASAPMessage(
                        SharkShiverComponent.SHARK_SHIVER_APP,
                        ShiverPaths.SHIVER_GROUP_UPDATE.getValue() + uriQueryParameter,
                        encryptedBytes
                );
            }
        }
    }

    private void publishGroupDelete(ShiverGroup group, List<CharSequence> members) throws ASAPException {
        byte[] emptyMessageBytes = new byte[0];

        for (CharSequence member : members) {
            if (member == ownPeer.getPeerID()) {
                continue;
            }

            byte[] encryptedBytes = shiverSecurity.encryptMessageContentForMemberOfGroup(
                    member,
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
            if (member == ownPeer.getPeerID()) {
                continue;
            }

            byte[] encryptedBytes = shiverSecurity.encryptMessageContentForMemberOfGroup(
                    member,
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
                    Log.writeLogErr(this, "Something went wrong when sending the message", e.getMessage());
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

                if (!groupStorage.hasGroupWithId(groupId)) {
                    Log.writeLogErr(this, "Does not know the group");
                    return;
                }

                ShiverGroup group = groupStorage.getGroup(groupId);
                if (!group.getMemberIdList().contains(ownPeer.getPeerID())) {
                    Log.writeLogErr(this, "Is not part of this group");
                    return;
                }

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
                        for (ShiverMessageReceiver messageReceiver : messageReceivers) {
                            messageReceiver.errorReceivingMessage(groupId, message, e);
                        }
                        Log.writeLogErr(this, "Could not decrypt message of member", e.getMessage());
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

                        ShiverGroup groupUpdate = groupFactory.parseGroupFromBytes(plainMessageBytes);

                        if (groupStorage.hasGroupWithId(groupId)) {
                            ShiverGroup group = groupStorage.getGroup(groupId);

                            try {
                                ShiverGroup updatedGroup = shiverMediator.mediate(group, groupUpdate);
                                groupStorage.storeGroup(updatedGroup);
                            } catch (ShiverNotSyncableException e) {
                                Log.writeLogErr(this, "Something went wrong when receiving group update", e.getMessage());
                                throw e;
                            }
                            return;
                        }

                        groupStorage.storeGroup(groupUpdate);
                    } catch (Exception e) {
                        Log.writeLogErr(this, "Something went wrong when receiving group update", e.getMessage());
                    }
                }
            }

            case SHIVER_GROUP_DELETE -> {
                String groupId = messageUri.getQuery().replace("group_id=", "");

                Iterator<byte[]> messages = asapMessages.getMessages();
                while (messages.hasNext()) {
                    byte[] message = messages.next();

                    try {
                        shiverSecurity.decryptMessageFromGroup(
                                ownPeer.getPeerID(),
                                groupId,
                                message
                        );

                        shiverSecurity.removeGroupKeys(groupId);
                        groupStorage.deleteGroup(groupId);
                    } catch (Exception e) {
                        Log.writeLogErr(this, "Could not decrypt, verify or invalidate member", e.getMessage());
                    }
                }

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
                        Log.writeLogErr(this, "Could not decrypt, verify or invalidate member", e.getMessage());
                    }
                }
            }
        }
    }
}
