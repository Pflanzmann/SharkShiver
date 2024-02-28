package com.shiver.models;


/**
 * An enum to help with the path for the needed ASAP uri
 * This helps to make sure every possible path is covered when handling the received message
 * It also prevents typos and other mistakes associated with strings
 */
public enum ShiverPaths {
    /**
     * Path representing the upflow phase
     */
    SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW("shark/shiver/group_credential_message/upflow"),

    /**
     * Path representing the broadcast phase
     */
    SHIVER_GROUP_CREDENTIAL_MESSAGE_BROADCAST("shark/shiver/group_credential_message/broadcast");
    private final String value;

    private ShiverPaths(String value) {
        this.value = value;
    }

    public CharSequence getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Method to parse a path associated with the path stored in the {@value } field of the enum cases
     *
     * @param charSequence uri path
     * @return associated element or null if no math could be find
     */
    public static ShiverPaths parsePathByValue(String charSequence) {
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
