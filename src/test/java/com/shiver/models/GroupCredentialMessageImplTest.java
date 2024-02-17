package com.shiver.models;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GroupCredentialMessageImplTest {

    @Test
    public void serializeTest() throws IOException, ClassNotFoundException {
        List<CharSequence> testPeers = new ArrayList<>();
        testPeers.add("testPeer1");
        testPeers.add("testPeer2");
        testPeers.add("testPeer3");
        testPeers.add("testPeer4");

        HashMap<CharSequence, byte[]> testKeys = new HashMap<>();
        testKeys.put("testPeer1", "testPeer1".getBytes());
        testKeys.put("testPeer2", "testPeer2".getBytes());
        testKeys.put("testPeer3", "testPeer3".getBytes());
        testKeys.put("testPeer4", "testPeer4".getBytes());

        GroupCredentialMessageImpl groupCredentialMessage = new GroupCredentialMessageImpl(
                "groupId",
                testPeers,
                testKeys
        );

        byte[] serializedMessage = groupCredentialMessage.serialize();

        GroupCredentialMessage deserializedMessage = GroupCredentialMessage.deserialize(serializedMessage);

        Assertions.assertEquals(groupCredentialMessage.getGroupId(), deserializedMessage.getGroupId());
        Assertions.assertArrayEquals(groupCredentialMessage.getPeerIds().toArray(), deserializedMessage.getPeerIds().toArray());
        Assertions.assertArrayEquals(groupCredentialMessage.getKeys().get("testPeer1"), deserializedMessage.getKeys().get("testPeer1"));
        Assertions.assertArrayEquals(groupCredentialMessage.getKeys().get("testPeer2"), deserializedMessage.getKeys().get("testPeer2"));
        Assertions.assertArrayEquals(groupCredentialMessage.getKeys().get("testPeer3"), deserializedMessage.getKeys().get("testPeer3"));
        Assertions.assertArrayEquals(groupCredentialMessage.getKeys().get("testPeer4"), deserializedMessage.getKeys().get("testPeer4"));
    }
}
