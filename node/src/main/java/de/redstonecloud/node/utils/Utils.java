package de.redstonecloud.node.utils;

import java.io.File;

public class Utils {
    public static void createBaseFolders() {
        String[] dirs = {"./servers", "./templates", "./tmp", "./logs", "./template_configs", "./types"};
        for (String dir : dirs) {
            File f = new File(dir);
            if (!f.exists()) {
                f.mkdir();
            }
        }
    }

    public static void setup() {
        System.out.println("Node setup not yet implemented. Creating basic folder structure...");
        createBaseFolders();
        
        try {
            java.nio.file.Files.writeString(java.nio.file.Paths.get(System.getProperty("user.dir") + "/.node.setup"),
                    "Node is set up. Do not delete this file or the setup will start again.");
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
