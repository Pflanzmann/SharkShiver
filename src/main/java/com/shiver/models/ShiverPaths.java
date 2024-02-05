package com.shiver.models;

public enum ShiverPaths {
    SHIVER_GROUP_UPDATE_URI("shark/shiver/group_update");

    private String value = null;

    private ShiverPaths(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
