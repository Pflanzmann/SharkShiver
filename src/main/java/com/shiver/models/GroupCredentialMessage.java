package com.shiver.models;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * The message that gets send when creating a new key.
 */
public interface GroupCredentialMessage extends Serializable {

    /**
     * Id of the group
     */
    public CharSequence getGroupId();

    /**
     * every member of the group. The order should NEVER get changed and should not get touched.
     * When implementing this we should never return the original List, always a copy of it
     */
    public List<CharSequence> getPeerIds();

    /**
     * All keys used for the DH associated to every peer
     */
    public HashMap<CharSequence, byte[]> getKeys();

    /**
     * Used to put in a new calculated key
     *
     * @param peerId - id to put it in for
     * @param key    - the new key
     */
    void putKeyForPeerId(CharSequence peerId, byte[] key);

    /**
     * method to serialize the object
     */
    byte[] serialize() throws IOException;

    /**
     * Static method to deserialize bytes into an {@link GroupCredentialMessageImpl} object
     *
     * @param messageBytes - raw bytes to serialize from
     */
    static GroupCredentialMessage deserialize(byte[] messageBytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(messageBytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (GroupCredentialMessageImpl) ois.readObject();
    }
}
