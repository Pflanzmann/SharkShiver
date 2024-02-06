package com.shiver;

import com.shiver.models.ShiverPaths;

import java.net.URI;

public class Mains {
    public static void main(String[] args) {
        URI messageUri = URI.create("shark/shiver/group_update?group_id=jnidsafkjn");

        ShiverPaths path = ShiverPaths.parsePathByValue(messageUri.getPath());
        String query = messageUri.getQuery().replace("group_id=", "");

        System.out.println("path: " + path);
        System.out.println("query: " + query);
    }
}