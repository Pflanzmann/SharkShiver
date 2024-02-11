package com.shiver.exceptions;

public class ShiverDecryptionFailedException extends Exception {
    public ShiverDecryptionFailedException() {
    }

    public ShiverDecryptionFailedException(String message) {
        super(message);
    }

    public ShiverDecryptionFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShiverDecryptionFailedException(Throwable cause) {
        super(cause);
    }

    public ShiverDecryptionFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
