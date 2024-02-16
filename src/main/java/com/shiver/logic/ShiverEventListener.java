package com.shiver.logic;

import com.shiver.components.ShiverComponent;
import com.shiver.models.GroupCredentialMessage;
import com.shiver.models.ShiverPaths;

/**
 * A listener for events that get emitted the {@link ShiverComponent}
 * Has to get registered at a sharkShiverComponent to receive events
 */
public interface ShiverEventListener {
    /**
     * Emits the {@link GroupCredentialMessage} when receiving one
     *
     * @param groupCredentialMessage
     */
    void onReceiveGroupCredentials(GroupCredentialMessage groupCredentialMessage);

    /**
     * Gets called when a groupCredentialMessage can not get handled and an error happens when receiving one
     *
     * @param path      - the path the message got received on
     * @param exception - exception that got thrown when working with the message
     */
    void onErrorReceivingGroupCredentialMessage(ShiverPaths path, Exception exception);

    /**
     * When a group key is ready to use this event gets called to notify every listener
     *
     * @param groupId - if of the group that is ready to use
     */
    void onReceivedGroupKey(CharSequence groupId);
}
