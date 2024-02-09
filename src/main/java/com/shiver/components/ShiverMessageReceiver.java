package com.shiver.components;

/**
 * Interface to implement if you want to use the SharkShiverComponent to receive messages
 * Register this receiver at your [SharkShiverComponent] implementation
 */
public interface ShiverMessageReceiver {

    /**
     * Function that gets called from the [SharkShiverComponent] when a message for a group got received
     *
     * @param groupId - id of the group the message got send with
     * @param content - the content of the message as byte array
     */
    void receiveShiverMessage(CharSequence groupId, byte[] content);

    /**
     * Function that gets called by the [SharkShiverComponent] when something went wrong
     *
     * @param groupId
     * @param encryptedMessage
     * @param exception
     */
    void errorReceivingMessage(CharSequence groupId, byte[] encryptedMessage, Exception exception);

}
