package com.shiver;

public interface ShiverSecurity {
    byte[] encryptAndSignMessage(String message);
    String decryptAndVerifyMessage(byte[] message);
}
