package com.shiver.storage;

import java.security.Key;
import java.util.HashMap;

/**
 * This class is an in memory implementation for the {@link ShiverKeyStorage}
 * All data gets lost when restarting the process
 */
public class ShiverKeyStoreInMemo implements ShiverKeyStorage {

    private final HashMap<CharSequence, Key> keys = new HashMap<>();

    @Override
    public void storeKeyForGroup(CharSequence groupId, Key key) {
        keys.put(groupId, key);
    }

    @Override
    public Key getKeyForGroup(CharSequence groupId) {
        return keys.get(groupId);
    }

    @Override
    public void deleteKeyForGroup(CharSequence groupId) {
        keys.remove(groupId);
    }
}
