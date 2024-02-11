package com.shiver.exceptions;

public class ShiverSendingSecretFailedException extends Exception {
    public ShiverSendingSecretFailedException() {
    }

    public ShiverSendingSecretFailedException(String message) {
        super(message);
    }

    public ShiverSendingSecretFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShiverSendingSecretFailedException(Throwable cause) {
        super(cause);
    }

    public ShiverSendingSecretFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
