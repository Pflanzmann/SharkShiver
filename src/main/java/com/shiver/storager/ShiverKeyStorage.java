package com.shiver.storager;

import java.security.Key;

/**
 * A storage for the done group keys
 */
public interface ShiverKeyStorage {
    /**
     * Stores a group key by an id
     *
     * @param groupId    - id to store for
     * @param privateKey - key to store
     */
    void storeKeyForGroup(CharSequence groupId, Key privateKey);

    /**
     * Fetches a key for a group
     *
     * @param groupId - id of the group
     * @return - returns null if there is no key
     */
    Key getKeyForGroup(CharSequence groupId);

    /**
     * deletes all keys associated with a groupId
     *
     * @param groupId - id to delete for
     */
    void deleteKeyForGroup(CharSequence groupId);
}
