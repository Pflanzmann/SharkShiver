package com.shiver.storager;

import java.security.Key;
import java.security.PrivateKey;

public interface ShiverKeyStorage {
    void storeKeyForGroup(CharSequence groupId, Key privateKey);

    Key getKeyForGroup(CharSequence groupId);

    void deleteKeyForGroup(CharSequence groupId);
}
