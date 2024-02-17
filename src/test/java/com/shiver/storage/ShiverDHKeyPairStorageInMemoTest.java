package com.shiver.storage;

import com.shiver.exceptions.ShiverDHKeyGenerationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

public class ShiverDHKeyPairStorageInMemoTest {

    @Test
    public void getOrGenerateKeyPairForGroup() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, ShiverDHKeyGenerationException {
        BigInteger p = new BigInteger("32317006071311007300338913926423828248817941241140239112842009751400741706634354222619689417363569347117901737909704191754605873209195028853758986185622153212175412514901774520270235796078236248884246189477587641105928646099411723245426622522193230540919037680524235519125679715870117001058055877651038861847280257976054903569732561526167081339361799541336476559160368317896729073178384589680639671900977202194168647225871031411336429319536193471636533209717077448227988588565369208645296636077250268955505928362751121174096972998068410554359584866583291642136218231078990999448652468262416972035911852507045361090559");
        BigInteger g = new BigInteger("2");
        int l = 2048;

        DHParameterSpec dhParamShared = new DHParameterSpec(p, g, l);

        CharSequence testGroupId1 = "group_id_1";
        CharSequence testGroupId2 = "group_id_2";

        ShiverDHKeyPairStorageInMemo shiverDHKeyPairStorageInMemo = new ShiverDHKeyPairStorageInMemo();
        KeyPair resultKeyPair1 = shiverDHKeyPairStorageInMemo.getOrGenerateKeyPairForGroup(testGroupId1);
        KeyPair resultKeyPair2 = shiverDHKeyPairStorageInMemo.getOrGenerateKeyPairForGroup(testGroupId2);

        KeyPair resultKeyPair1_again1 = shiverDHKeyPairStorageInMemo.getOrGenerateKeyPairForGroup(testGroupId1);
        KeyPair resultKeyPair1_again2 = shiverDHKeyPairStorageInMemo.getOrGenerateKeyPairForGroup(testGroupId1);

        shiverDHKeyPairStorageInMemo.deleteKeyPairForGroupId(testGroupId1);
        KeyPair resultKeyPair1_deleted = shiverDHKeyPairStorageInMemo.getOrGenerateKeyPairForGroup(testGroupId1);

        DHParameterSpec resultKeyPairParams = ((DHPublicKey) resultKeyPair1.getPublic()).getParams();
        Assertions.assertEquals(dhParamShared.getG(), resultKeyPairParams.getG());
        Assertions.assertEquals(dhParamShared.getP(), resultKeyPairParams.getP());
        Assertions.assertEquals(dhParamShared.getL(), resultKeyPairParams.getL());

        Assertions.assertNotEquals(resultKeyPair1, resultKeyPair2);

        Assertions.assertEquals(resultKeyPair1, resultKeyPair1_again1);
        Assertions.assertEquals(resultKeyPair1, resultKeyPair1_again2);
        Assertions.assertNotEquals(resultKeyPair1, resultKeyPair1_deleted);
        Assertions.assertNotEquals(resultKeyPair2, resultKeyPair1_deleted);
    }
}
