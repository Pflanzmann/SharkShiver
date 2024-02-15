package com.shiver.exceptions;

public class ShiverDHKeyGenerationException extends Exception {
    public ShiverDHKeyGenerationException() {
    }

    public ShiverDHKeyGenerationException(String message) {
        super(message);
    }

    public ShiverDHKeyGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShiverDHKeyGenerationException(Throwable cause) {
        super(cause);
    }

    public ShiverDHKeyGenerationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
