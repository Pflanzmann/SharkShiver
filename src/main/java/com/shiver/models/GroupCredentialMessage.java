package com.shiver.models;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public interface GroupCredentialMessage extends Serializable {

    public CharSequence getGroupId();

    public List<CharSequence> getPeerIds();

    public HashMap<CharSequence, byte[]> getKeys();

    public void putKeyForPeerId(CharSequence peerId, byte[] key);

    public byte[] serialize() throws IOException;

    public static GroupCredentialMessage deserialize(byte[] messageBytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(messageBytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (GroupCredentialMessageImpl) ois.readObject();
    }
}
