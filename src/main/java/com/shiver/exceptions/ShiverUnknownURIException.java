package com.shiver.exceptions;

public class ShiverUnknownURIException extends Exception {
    public ShiverUnknownURIException() {
    }

    public ShiverUnknownURIException(String message) {
        super(message);
    }

    public ShiverUnknownURIException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShiverUnknownURIException(Throwable cause) {
        super(cause);
    }

    public ShiverUnknownURIException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
