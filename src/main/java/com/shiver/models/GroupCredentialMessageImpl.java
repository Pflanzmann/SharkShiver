package com.shiver.models;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GroupCredentialMessageImpl implements GroupCredentialMessage, Serializable {
    private final CharSequence groupId;
    private final List<CharSequence> peerIds;
    private final HashMap<CharSequence, byte[]> keys;

    public GroupCredentialMessageImpl(CharSequence groupId, List<CharSequence> peerIds, HashMap<CharSequence, byte[]> keys) {
        this.groupId = groupId;
        this.peerIds = peerIds;
        this.keys = keys;
    }

    public CharSequence getGroupId() {
        return groupId;
    }

    public List<CharSequence> getPeerIds() {
        return new ArrayList<>(peerIds);
    }

    public HashMap<CharSequence, byte[]> getKeys() {
        return new HashMap<>(keys);
    }

    @Override
    public void putKeyForPeerId(CharSequence peerId, byte[] key) {
        keys.put(peerId, key);
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this);
        oos.flush();
        return baos.toByteArray();
    }

    /**
     * Static method to deserialize bytes into an {@link GroupCredentialMessageImpl} object
     *
     * @param messageBytes - raw bytes to serialize from
     */
    public static GroupCredentialMessage deserialize(byte[] messageBytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(messageBytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (GroupCredentialMessageImpl) ois.readObject();
    }
}
