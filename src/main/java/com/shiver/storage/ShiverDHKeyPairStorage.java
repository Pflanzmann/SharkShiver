package com.shiver.storage;

import com.shiver.exceptions.ShiverDHKeyGenerationException;

import java.security.KeyPair;

/**
 * This interface helps to store DH KeyPairs for the process of exchanging keys
 */
public interface ShiverDHKeyPairStorage {
    /**
     * Fetches an already stored key associated with a groupId or generates a new one and stores it
     *
     * @param groupId - id associated with the key
     * @return - KeyPair to do the DH with
     * @throws ShiverDHKeyGenerationException - if something fails when generating a new key
     */
    KeyPair getOrGenerateKeyPairForGroup(CharSequence groupId) throws ShiverDHKeyGenerationException;

    /**
     * Delete the key for the group
     *
     * @param groupId - id of the group
     */
    void deleteKeyPairForGroupId(CharSequence groupId);
}
