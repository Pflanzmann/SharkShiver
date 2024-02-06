package com.shiver.components;

import com.shiver.exceptions.NoGroupAvailableException;
import com.shiver.exceptions.ShiverPermissionDeniedException;
import com.shiver.models.Group;
import net.sharksystem.ASAPFormats;
import net.sharksystem.SharkComponent;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessages;

import java.io.IOException;

@ASAPFormats(formats = {SharkShiverComponent.SHARK_SHIVER_APP})
public interface SharkShiverComponent extends SharkComponent {
    String SHARK_SHIVER_APP = "SharkShiverApp";

    Group createGroup() throws ASAPException;

    void addPeerToGroup(CharSequence groupId, CharSequence groupMemberId) throws ASAPException, NoGroupAvailableException, ShiverPermissionDeniedException, IOException;

    void removePeerFromGroup(CharSequence groupId, CharSequence groupMemberId) throws ASAPException, NoGroupAvailableException, ShiverPermissionDeniedException, IOException;

    void deleteGroup(CharSequence groupId) throws ASAPException, NoGroupAvailableException, ShiverPermissionDeniedException, IOException;

    void sendGroupMessage(CharSequence groupId, ASAPMessages messages) throws ASAPException, NoGroupAvailableException, IOException;
}
