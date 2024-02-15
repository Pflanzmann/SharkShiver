package com.shiver.storager;

import java.security.Key;
import java.util.HashMap;

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
