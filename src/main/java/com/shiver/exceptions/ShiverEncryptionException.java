package com.shiver.exceptions;

public class ShiverEncryptionException extends Exception {
    public ShiverEncryptionException() {
    }

    public ShiverEncryptionException(String message) {
        super(message);
    }

    public ShiverEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShiverEncryptionException(Throwable cause) {
        super(cause);
    }

    public ShiverEncryptionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
