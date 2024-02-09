package com.shiver;

import com.shiver.models.Group;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPSecurityException;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;
import net.sharksystem.asap.pki.CredentialMessageInMemo;
import net.sharksystem.pki.CredentialMessage;
import net.sharksystem.pki.SharkCredentialReceivedListener;
import net.sharksystem.pki.SharkPKIComponent;
import net.sharksystem.utils.Log;

import java.io.IOException;
import java.util.List;

public class SharkPkiSecurity implements ShiverSecurity, SharkCredentialReceivedListener {
    private final SharkPKIComponent sharkPKIComponent;
    private final GroupStorage groupStorage;

    public SharkPkiSecurity(SharkPKIComponent sharkPKIComponent, GroupStorage groupStorage) {
        this.sharkPKIComponent = sharkPKIComponent;
        this.groupStorage = groupStorage;

        sharkPKIComponent.setSharkCredentialReceivedListener(this);
    }

    @Override
    public void credentialReceived(CredentialMessage credentialMessage) {
        CharSequence subjectId = credentialMessage.getSubjectID();
        CharSequence memberId = extractMemberIdFromCombinedId(subjectId);
        CharSequence groupId = extractMemberIdFromCombinedId(subjectId);

        List<Group> groups = groupStorage.getAllGroups();

        //Check all every group
        for (Group group : groups) {
            if (group.getGroupId() == groupId) {

                //Check every member of group
                for (CharSequence groupMemberId : group.getMemberIdList()) {
                    if (groupMemberId == memberId) {
                        try {
                            sharkPKIComponent.acceptAndSignCredential(credentialMessage);
                        } catch (IOException | ASAPSecurityException e) {
                            Log.writeLog(this, "Something went wrong when accepting message");
                        }
                    }
                }

                Log.writeLog(this, "No member associated with the member and group of the CredentialMessage");
            }
        }

        Log.writeLog(this, "No group associated with the group of the CredentialMessage");
    }

    @Override
    public boolean isSecretExchangeNeeded(CharSequence groupId, CharSequence memberId) throws ASAPSecurityException {
        CharSequence membershipId = combineMemberAndGroupId(memberId, groupId);
        return sharkPKIComponent.getPublicKey(membershipId) != null;
    }

    @Override
    public void sendSecretToMemberOfGroup(CharSequence groupId, CharSequence memberId) throws IOException, ASAPException {
        CharSequence membershipId = combineMemberAndGroupId(memberId, groupId);
        CredentialMessageInMemo credentialMessage = new CredentialMessageInMemo(membershipId, sharkPKIComponent.getOwnerName(), sharkPKIComponent.getKeysCreationTime(), sharkPKIComponent.getPublicKey());

        sharkPKIComponent.sendOnlineCredentialMessage(credentialMessage);
    }

    @Override
    public byte[] encryptMessageContentForMemberOfGroup(CharSequence memberId, CharSequence groupId, byte[] message) throws ASAPSecurityException {
        CharSequence combinedId = combineMemberAndGroupId(memberId, groupId);

        return ASAPCryptoAlgorithms.produceEncryptedMessagePackage(message, combinedId, sharkPKIComponent);
    }

    @Override
    public byte[] decryptMessageFromGroup(CharSequence senderId, CharSequence groupId, byte[] message) throws ASAPException, IOException {
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
}
