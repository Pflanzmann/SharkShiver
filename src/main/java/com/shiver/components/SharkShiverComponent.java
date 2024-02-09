package com.shiver.components;

import com.shiver.exceptions.ShiverNoGroupException;
import com.shiver.exceptions.ShiverPermissionDeniedException;
import com.shiver.models.ShiverGroup;
import net.sharksystem.ASAPFormats;
import net.sharksystem.SharkComponent;
import net.sharksystem.asap.ASAPException;

import java.io.IOException;
import java.util.List;

/**
 * This interface is the main interface between the user and this library.
 * The user should primarily use this interface to manage and manipulate groups, not the underlying classes.
 */
@ASAPFormats(formats = {SharkShiverComponent.SHARK_SHIVER_APP})
public interface SharkShiverComponent extends SharkComponent {
    String SHARK_SHIVER_APP = "SharkShiverApp";

    /**
     * Joins the group object and stores it
     *
     * @throws ASAPException - throws when something went wrong in the underlying ASAP structure
     */
    ShiverGroup createGroup() throws ASAPException;

    /**
     * returns all groups that are handled by the associated group storage
     *
     * @return - A list containing all groups known to the component
     */
    List<ShiverGroup> getAllGroups();

    /**
     * Returns the Ids of all peers associated with the group
     *
     * @param groupId - the id of the group
     * @return - a new set containing all peer ids
     * @throws ShiverNoGroupException - throws when there is no group found associated to the id
     */
    List<CharSequence> getAllMembersOfGroup(CharSequence groupId) throws ShiverNoGroupException;

    /**
     * Adds the given peer to the group and publishes a group update.
     * This method adds a peer to the group.
     * The invitation and acceptance process should be managed before interacting with
     * this component, as it can vary greatly depending on the use case of the component.
     *
     * @param groupId - the id of the group that the peer should join
     * @param peerId  - the id of the peer that should be added
     * @throws ASAPException                   - throws when something went wrong in the underlying ASAP structure
     * @throws ShiverNoGroupException          - throws when there is no group to add a peer to
     * @throws ShiverPermissionDeniedException - throws when the user is in this group not permitted to execute this function
     * @throws IOException                     - throws when something went wrong in the underlying ASAP structure
     */
    void addPeerToGroup(CharSequence groupId, CharSequence peerId) throws ASAPException, ShiverNoGroupException, ShiverPermissionDeniedException, IOException;

    /**
     * Removes a member from a group and publishes a group update
     *
     * @param groupId  - the id the member should get removed from
     * @param memberId - the id of the member that should get deleted
     * @throws ASAPException                   - throws when something went wrong in the underlying ASAP structure
     * @throws ShiverNoGroupException          - throws when there is no group to remove a peer from
     * @throws ShiverPermissionDeniedException - throws when the user is in this group not permitted to execute this function
     * @throws IOException                     - throws when something went wrong in the underlying ASAP structure
     */
    void removePeerFromGroup(CharSequence groupId, CharSequence memberId) throws ASAPException, ShiverNoGroupException, ShiverPermissionDeniedException, IOException;

    /**
     * Deletes the group and sends out a delete group notification to all members
     *
     * @param groupId - The id of the group that should get deleted
     * @throws ASAPException                   - throws when something went wrong in the underlying ASAP structure
     * @throws ShiverNoGroupException          - throws when there is no group to delete
     * @throws ShiverPermissionDeniedException - throws when the user is in this group not permitted to execute this function
     * @throws IOException                     - throws when something went wrong in the underlying ASAP structure
     */
    void deleteGroup(CharSequence groupId) throws ASAPException, ShiverNoGroupException, ShiverPermissionDeniedException, IOException;

    /**
     * Sends a message to all group members
     *
     * @param groupId - The id of the group that the message should get published for
     * @param message - the message that the user wants to send
     * @throws ASAPException          - throws when something went wrong in the underlying ASAP structure
     * @throws ShiverNoGroupException - throws when there is no group to send a message to
     * @throws IOException            - throws when something went wrong in the underlying ASAP structure
     */
    void sendGroupMessage(CharSequence groupId, byte[] message) throws ASAPException, ShiverNoGroupException, IOException;

    /**
     * Invalidates all secrets associated with a member of a group.
     * This is used when keys get compromised and you want to refresh the keys of this user
     *
     * @param memberId - the id of the peer
     * @param groupId  - the id of the group of the member
     * @throws ShiverNoGroupException          - throws when there is no group associated with the id
     * @throws ShiverPermissionDeniedException - throws when the user is in this group not permitted to execute this function
     * @throws IOException                     - throws when something went wrong in the underlying ASAP structure
     * @throws ASAPException                   - throws when something went wrong in the underlying ASAP structure
     */
    void invalidateMemberForGroup(CharSequence memberId, CharSequence groupId) throws ShiverNoGroupException, ShiverPermissionDeniedException, IOException, ASAPException;

    /**
     * Publishes a group update if the user wants update the group manually
     *
     * @param groupId - id of the group to publish
     * @throws ASAPException          - throws when something went wrong in the underlying ASAP structure
     * @throws ShiverNoGroupException - throws when there is no group associated with the id
     */
    void publishGroupUpdate(CharSequence groupId) throws ASAPException, ShiverNoGroupException, IOException;

    /**
     * Publishes a group update for currently online users only if the user wants update the group manually
     *
     * @param groupId - id of the group to publish
     * @throws ASAPException          - throws when something went wrong in the underlying ASAP structure
     * @throws ShiverNoGroupException - throws when there is no group associated with the id
     * @throws IOException
     */
    void publishOnlineOnlyGroupUpdate(CharSequence groupId) throws ASAPException, ShiverNoGroupException, IOException;

    /**
     * Adds a receiver for messages that got send with the sendGroupMessage(...) function
     *
     * @param shiverMessageReceiver - the class implementing the interface
     */
    void addShiverMessageReceiver(ShiverMessageReceiver shiverMessageReceiver);

    /**
     * Removes a receiver for messages that got send with the sendGroupMessage(...) function
     *
     * @param shiverMessageReceiver - the class implementing the interface
     */
    void removeShiverMessageReceiver(ShiverMessageReceiver shiverMessageReceiver);
}
