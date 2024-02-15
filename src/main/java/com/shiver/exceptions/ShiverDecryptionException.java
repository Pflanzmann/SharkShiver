package com.shiver.exceptions;

public class ShiverDecryptionException extends Exception {
    public ShiverDecryptionException() {
    }

    public ShiverDecryptionException(String message) {
        super(message);
    }

    public ShiverDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShiverDecryptionException(Throwable cause) {
        super(cause);
    }

    public ShiverDecryptionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
