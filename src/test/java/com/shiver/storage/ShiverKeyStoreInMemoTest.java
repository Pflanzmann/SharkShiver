package com.shiver.storage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.Key;

public class ShiverKeyStoreInMemoTest {

    @Test
    public void test() {
        Key testKey1 = Mockito.mock(Key.class);
        Key testKey2 = Mockito.mock(Key.class);

        CharSequence testGroupId1 = "GroupId1";
        CharSequence testGroupId2 = "GroupId2";

        ShiverKeyStoreInMemo shiverKeyStoreInMemo = new ShiverKeyStoreInMemo();
        Key nullKey1 = shiverKeyStoreInMemo.getKeyForGroup(testGroupId1);
        Key nullKey2 = shiverKeyStoreInMemo.getKeyForGroup(testGroupId2);

        shiverKeyStoreInMemo.storeKeyForGroup(testGroupId1, testKey1);
        Key resultKey1 = shiverKeyStoreInMemo.getKeyForGroup(testGroupId1);
        Key nullKey2_again = shiverKeyStoreInMemo.getKeyForGroup(testGroupId2);

        shiverKeyStoreInMemo.storeKeyForGroup(testGroupId2, testKey2);
        Key resultKey1_again = shiverKeyStoreInMemo.getKeyForGroup(testGroupId1);
        Key resultKey2 = shiverKeyStoreInMemo.getKeyForGroup(testGroupId2);

        shiverKeyStoreInMemo.deleteKeyForGroup(testGroupId1);
        shiverKeyStoreInMemo.deleteKeyForGroup(testGroupId2);
        Key deletedKey1 = shiverKeyStoreInMemo.getKeyForGroup(testGroupId1);
        Key deletedKey2 = shiverKeyStoreInMemo.getKeyForGroup(testGroupId1);

        Assertions.assertNull(nullKey1);
        Assertions.assertNull(nullKey2);

        Assertions.assertEquals(testKey1, resultKey1);
        Assertions.assertNull(nullKey2_again);

        Assertions.assertEquals(testKey1, resultKey1_again);
        Assertions.assertEquals(testKey2, resultKey2);

        Assertions.assertNull(deletedKey1);
        Assertions.assertNull(deletedKey2);
    }
}
