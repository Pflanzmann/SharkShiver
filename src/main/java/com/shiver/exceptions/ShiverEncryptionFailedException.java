package com.shiver.exceptions;

public class ShiverEncryptionFailedException extends Exception {
    public ShiverEncryptionFailedException() {
    }

    public ShiverEncryptionFailedException(String message) {
        super(message);
    }

    public ShiverEncryptionFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShiverEncryptionFailedException(Throwable cause) {
        super(cause);
    }

    public ShiverEncryptionFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
