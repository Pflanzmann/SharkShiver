package com.shiver.exceptions;

public class ShiverGroupSizeException extends Exception {
    public ShiverGroupSizeException() {
    }

    public ShiverGroupSizeException(String message) {
        super(message);
    }

    public ShiverGroupSizeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShiverGroupSizeException(Throwable cause) {
        super(cause);
    }

    public ShiverGroupSizeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
