package com.shiver.exceptions;

public class ShiverPeerNotVerifiedException extends Exception {
    public ShiverPeerNotVerifiedException() {
    }

    public ShiverPeerNotVerifiedException(String message) {
        super(message);
    }

    public ShiverPeerNotVerifiedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShiverPeerNotVerifiedException(Throwable cause) {
        super(cause);
    }

    public ShiverPeerNotVerifiedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
