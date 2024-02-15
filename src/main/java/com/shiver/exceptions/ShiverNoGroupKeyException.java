package com.shiver.exceptions;

public class ShiverNoGroupKeyException extends Exception {
    public ShiverNoGroupKeyException() {
    }

    public ShiverNoGroupKeyException(String message) {
        super(message);
    }

    public ShiverNoGroupKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShiverNoGroupKeyException(Throwable cause) {
        super(cause);
    }

    public ShiverNoGroupKeyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
