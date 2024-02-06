package com.shiver.models;

public enum ShiverPaths {
    SHIVER_GROUP_Message("shark/shiver/group_message"),
    SHIVER_GROUP_UPDATE("shark/shiver/group_update"),
    SHIVER_GROUP_DELETE("shark/shiver/group_delete");

    private CharSequence value = null;

    private ShiverPaths(String value) {
        this.value = value;
    }

    public CharSequence getValue() {
        return value;
    }

    public static ShiverPaths parsePathByValue(CharSequence charSequence) {
        ShiverPaths shiverPath = null;

        // Find the corresponding enum for the string value
        for (ShiverPaths sp : ShiverPaths.values()) {
            if (sp.getValue().equals(charSequence)) {
                shiverPath = sp;
                break;
            }
        }

        return shiverPath;
    }
    }
