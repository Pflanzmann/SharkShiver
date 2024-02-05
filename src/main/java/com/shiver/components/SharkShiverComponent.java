package com.shiver.components;

import net.sharksystem.SharkComponent;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPMessages;
import com.shiver.exceptions.NoGroupAvailableException;
import com.shiver.models.Group;
import com.shiver.models.GroupMember;

import java.io.IOException;

public interface SharkShiverComponent extends SharkComponent {
    CharSequence SHARK_SHIVER_APP = "SharkShiverApp";

    Group createGroup() throws ASAPException;

    void addPeerToGroup(GroupMember groupMember) throws ASAPException, NoGroupAvailableException, IOException;

    void removePeerFromGroup(GroupMember groupMember) throws ASAPException, NoGroupAvailableException;

    void sendGroupMessage(ASAPMessages messages) throws ASAPException, NoGroupAvailableException, IOException;

    void deleteGroup() throws ASAPException, NoGroupAvailableException;
}
