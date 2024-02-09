package com.shiver;

import com.shiver.models.ShiverGroup;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.asap.pki.CredentialMessageInMemo;
import net.sharksystem.pki.CredentialMessage;
import net.sharksystem.pki.SharkCredentialReceivedListener;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is the main implementation of the [ShiverSecurity] interface.
 * This class has no need to be loaded or stored because all data is handled by the [SharkPKIComponent] and [GroupStorage]
 */
public class SharkPkiSecurity implements ShiverSecurity, SharkCredentialReceivedListener {
    private final SharkPKIComponent sharkPKIComponent;
    private final ShiverGroupStorage groupStorage;
    private List<InvitedGroup> groupInvites = new ArrayList<>();

    public SharkPkiSecurity(SharkPKIComponent sharkPKIComponent, ShiverGroupStorage groupStorage) {
        this.sharkPKIComponent = sharkPKIComponent;
        this.groupStorage = groupStorage;

        sharkPKIComponent.setSharkCredentialReceivedListener(this);
    }

    @Override
    public void credentialReceived(CredentialMessage credentialMessage) {
        if (checkInvites(credentialMessage)) {
            return;
        }

        if (checkGroups(credentialMessage)) {
            return;
        }
    }

    private boolean checkInvites(CredentialMessage credentialMessage) {
        CharSequence subjectId = credentialMessage.getSubjectID();
        CharSequence memberId = extractMemberIdFromCombinedId(subjectId);
        CharSequence groupId = extractGroupIdFromCombinedId(subjectId);

        InvitedGroup foundInvite = null;
        for (InvitedGroup invitedGroup : groupInvites) {
            if (invitedGroup.groupId == groupId && invitedGroup.inviteIssuerId == memberId) {
                foundInvite = invitedGroup;
            }
        }
        if (foundInvite != null) {
            groupInvites.remove(foundInvite);
            try {
                sharkPKIComponent.acceptAndSignCredential(credentialMessage);
            } catch (IOException | ASAPSecurityException e) {
                Log.writeLog(this, "Something went wrong when accepting message");
            }
            return true;
        }

        return false;
    }

    private boolean checkGroups(CredentialMessage credentialMessage) {
        CharSequence subjectId = credentialMessage.getSubjectID();
        CharSequence memberId = extractMemberIdFromCombinedId(subjectId);
        CharSequence groupId = extractGroupIdFromCombinedId(subjectId);

        List<ShiverGroup> groups = groupStorage.getAllGroups();
        //Check every group
        for (ShiverGroup group : groups) {
            if (group.getGroupId() == groupId) {

                //Check every member of group
                for (CharSequence groupMemberId : group.getMemberIdList()) {
                    if (groupMemberId == memberId) {
                        try {
                            sharkPKIComponent.acceptAndSignCredential(credentialMessage);
                            return true;
                        } catch (IOException | ASAPSecurityException e) {
                            Log.writeLog(this, "Something went wrong when accepting message");
                        }
                    }
                }

                Log.writeLog(this, "No member associated with the member and group of the CredentialMessage");
            }
        }

        return false;
    }

    @Override
    public boolean isSecretExchangeNeeded(CharSequence groupId, CharSequence memberId) throws ASAPSecurityException {
        CharSequence membershipId = combineMemberAndGroupId(memberId, groupId);
        return sharkPKIComponent.getPublicKey(membershipId) != null;
    }

    @Override
    public void acceptGroupInvite(CharSequence groupId, CharSequence memberId) {
        groupInvites.add(new InvitedGroup(groupId, memberId));
    }

    @Override
    public void sendSecretToMemberOfGroup(CharSequence groupId, CharSequence ownMemberId) throws IOException, ASAPException {
        CharSequence membershipId = combineMemberAndGroupId(ownMemberId, groupId);
        CredentialMessageInMemo credentialMessage = new CredentialMessageInMemo(membershipId, sharkPKIComponent.getOwnerName(), sharkPKIComponent.getKeysCreationTime(), sharkPKIComponent.getPublicKey());

        sharkPKIComponent.sendOnlineCredentialMessage(credentialMessage);
    }

    @Override
    public byte[] encryptMessageContentForMemberOfGroup(CharSequence recipient, CharSequence groupId, byte[] message) throws ASAPSecurityException {
        CharSequence combinedId = combineMemberAndGroupId(recipient, groupId);

        return ASAPCryptoAlgorithms.produceEncryptedMessagePackage(message, combinedId, sharkPKIComponent);
    }

    @Override
    public byte[] decryptMessageFromGroup(CharSequence senderId, CharSequence groupId, byte[] message) throws IOException, ASAPException {
        ASAPCryptoAlgorithms.EncryptedMessagePackage encryptedMessagePackage = ASAPCryptoAlgorithms.parseEncryptedMessagePackage(message);

        return ASAPCryptoAlgorithms.decryptPackage(encryptedMessagePackage, sharkPKIComponent);
    }

    @Override
    public void invalidateSecretsOfMemberInGroup(CharSequence memberId, CharSequence groupId) {
        // SharkPKI can not do that yet
    }

    @Override
    public void removeGroupKeys(CharSequence groupId) {
        // SharkPKI can not do that yet
    }

    private CharSequence combineMemberAndGroupId(CharSequence memberId, CharSequence groupId) {
        return memberId + "#" + groupId;
    }

    private CharSequence extractMemberIdFromCombinedId(CharSequence combinedId) {
        return combinedId.toString().split("#")[0];
    }

    private CharSequence extractGroupIdFromCombinedId(CharSequence combinedId) {
        return combinedId.toString().split("#")[1];
    }

    private class InvitedGroup {
        private CharSequence groupId;
        private CharSequence inviteIssuerId;

        public InvitedGroup(CharSequence groupId, CharSequence inviteIssuerId) {
            this.groupId = groupId;
            this.inviteIssuerId = inviteIssuerId;
        }
    }
}
