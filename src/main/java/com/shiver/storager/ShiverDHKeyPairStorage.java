package com.shiver.storager;

import com.shiver.exceptions.ShiverDHKeyGenerationException;

import java.security.KeyPair;

public interface ShiverDHKeyPairStorage {
    void storeKeyPairByGroupId(CharSequence groupId, KeyPair keyPair);

    KeyPair getOrGenerateKeyPairForGroup(CharSequence groupId) throws ShiverDHKeyGenerationException;

    void deleteKeyPairForGroupId(CharSequence groupId);
}
