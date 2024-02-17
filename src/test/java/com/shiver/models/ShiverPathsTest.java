package com.shiver.models;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ShiverPathsTest {

    @Test
    public void parse() {
        String correctUpflow = "shark/shiver/group_credential_message/upflow";
        String correctBroadcast = "shark/shiver/group_credential_message/broadcast";
        String wrongPath = "SomeWrongPath";

        ShiverPaths upflow = ShiverPaths.parsePathByValue(correctUpflow);
        ShiverPaths broadcast = ShiverPaths.parsePathByValue(correctBroadcast);
        ShiverPaths shouldBeNull = ShiverPaths.parsePathByValue(wrongPath);

        Assertions.assertEquals(ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_UPFLOW, upflow);
        Assertions.assertEquals(ShiverPaths.SHIVER_GROUP_CREDENTIAL_MESSAGE_BROADCAST, broadcast);
        Assertions.assertNull(shouldBeNull);
    }
}
