package com.shiver.components;

public interface ShiverMessageReceiver {
    void receiveShiverMessage(CharSequence groupId, byte[] content);
}
