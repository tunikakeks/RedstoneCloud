package de.redstonecloud.node.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NodeConfig {
    private static JsonObject cfg;

    public static JsonObject getCfg() {
        return getCfg(false);
    }

    public static JsonObject getCfg(boolean reload) {
        if (reload || cfg == null) {
            try {
                String workingDir = System.getProperty("user.dir");
                cfg = new Gson().fromJson(Files.readString(Paths.get(workingDir + "/node.json")), JsonObject.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return cfg;
    }

    public static String getNodeName() {
        return getCfg().get("node_name").getAsString();
    }

    public static String getAuthKey() {
        return getCfg().get("auth_key").getAsString();
    }

    public static String getMasterChannel() {
        return getCfg().get("master_channel").getAsString();
    }

    public static String getRedisIp() {
        return getCfg().get("redis_bind").getAsString();
    }

    public static String getRedisPort() {
        return getCfg().get("redis_port").getAsString();
    }

    public static int getRedisDb() {
        return !getCfg().has("redis_db") ? 0 : getCfg().get("redis_db").getAsInt();
    }
}
