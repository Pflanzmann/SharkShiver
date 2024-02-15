package com.shiver.storager;

import com.shiver.exceptions.ShiverDHKeyGenerationException;

import javax.crypto.spec.DHParameterSpec;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.HashMap;

public class ShiverDHKeyPairStorageInMemo implements ShiverDHKeyPairStorage {

    private final HashMap<CharSequence, KeyPair> keypairs = new HashMap<>();

    private BigInteger p = new BigInteger("32317006071311007300338913926423828248817941241140239112842009751400741706634354222619689417363569347117901737909704191754605873209195028853758986185622153212175412514901774520270235796078236248884246189477587641105928646099411723245426622522193230540919037680524235519125679715870117001058055877651038861847280257976054903569732561526167081339361799541336476559160368317896729073178384589680639671900977202194168647225871031411336429319536193471636533209717077448227988588565369208645296636077250268955505928362751121174096972998068410554359584866583291642136218231078990999448652468262416972035911852507045361090559");
    private BigInteger g = new BigInteger("2");
    private int l = 2048;

    @Override

    public void storeKeyPairByGroupId(CharSequence groupId, KeyPair keyPair) {
        keypairs.put(groupId, keyPair);
    }

    @Override
    public KeyPair getOrGenerateKeyPairForGroup(CharSequence groupId) throws ShiverDHKeyGenerationException {
        if (keypairs.containsKey(groupId)) {
            return keypairs.get(groupId);
        } else {
            try {
                DHParameterSpec dhParamShared = new DHParameterSpec(p, g, l);
                KeyPairGenerator gen = KeyPairGenerator.getInstance("DH");
                gen.initialize(dhParamShared);
                KeyPair keyPair = gen.generateKeyPair();
                keypairs.put(groupId, keyPair);
                return keyPair;
            } catch (Exception e) {
                throw new ShiverDHKeyGenerationException(e);
            }
        }
    }

    @Override
    public void deleteKeyPairForGroupId(CharSequence groupId) {
        keypairs.remove(groupId);
    }
}
