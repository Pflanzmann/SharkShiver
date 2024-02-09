package com.shiver.components;

import com.shiver.exceptions.ShiverNoGroupException;
import com.shiver.exceptions.ShiverPermissionDeniedException;
import com.shiver.models.Group;
import net.sharksystem.ASAPFormats;
import net.sharksystem.SharkComponent;
import net.sharksystem.asap.ASAPException;

import java.io.IOException;

@ASAPFormats(formats = {SharkShiverComponent.SHARK_SHIVER_APP})
public interface SharkShiverComponent extends SharkComponent {
    String SHARK_SHIVER_APP = "SharkShiverApp";

    Group createGroup() throws ASAPException;

    void addPeerToGroup(CharSequence groupId, CharSequence groupMemberId) throws ASAPException, ShiverNoGroupException, ShiverPermissionDeniedException, IOException;

    void removePeerFromGroup(CharSequence groupId, CharSequence groupMemberId) throws ASAPException, ShiverNoGroupException, ShiverPermissionDeniedException, IOException;

    void deleteGroup(CharSequence groupId) throws ASAPException, ShiverNoGroupException, ShiverPermissionDeniedException, IOException;

    void sendGroupMessage(CharSequence groupId, byte[] message) throws ASAPException, ShiverNoGroupException, IOException;

    void invalidateMemberForGroup(CharSequence memberId, CharSequence groupId) throws ShiverNoGroupException, ShiverPermissionDeniedException, IOException, ASAPException;

    void addShiverMessageReceiver(ShiverMessageReceiver shiverMessageReceiver);

    void removeShiverMessageReceiver(ShiverMessageReceiver shiverMessageReceiver);
}
