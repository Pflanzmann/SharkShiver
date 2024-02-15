package com.shiver.models;

public enum ShiverPaths {
    SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW("shark/shiver/group_credential_message/upflow"),
    SHIVER_GROUP_CREDENTIAL_MESSAGE_BROADCAST("shark/shiver/group_credential_message/broadcast");

    private final CharSequence value;

    private ShiverPaths(String value) {
        this.value = value;
    }

    public CharSequence getValue() {
        return value;
    }

    public static ShiverPaths parsePathByValue(CharSequence charSequence) {
        ShiverPaths shiverPath = null;

        for (ShiverPaths sp : ShiverPaths.values()) {
            if (sp.getValue().equals(charSequence)) {
                shiverPath = sp;
                break;
            }
        }

        return shiverPath;
    }
}
