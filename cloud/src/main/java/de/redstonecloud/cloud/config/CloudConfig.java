package de.redstonecloud.cloud.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.config.entry.ClusterEntry;
import de.redstonecloud.cloud.config.entry.ClusterMode;
import de.redstonecloud.cloud.config.entry.RedisEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CloudConfig {
    private static JsonObject cfg;

    public static JsonObject getCfg() {
        return getCfg(false);
    }

    public static JsonObject getCfg(boolean reload) {
        if (reload || cfg == null) {
            try {
                cfg = new Gson().fromJson(Files.readString(Paths.get(RedstoneCloud.workingDir + "/cloud.json")), JsonObject.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return cfg;
    }

    public static RedisEntry getRedis() {
        JsonObject cfg = getCfg();
        return new RedisEntry(
                cfg.get("redis_bind").getAsString(),
                cfg.get("redis_port").getAsString(),
                !cfg.get("custom_redis").getAsBoolean(),
                !cfg.has("redis_db") ? 0 : cfg.get("redis_db").getAsInt()
        );
    }

    public static ClusterEntry getCluster() {
        JsonObject cfg = getCfg();
        if (!cfg.has("cluster")) {
            // Default to standalone mode if cluster config is missing
            return new ClusterEntry(false, ClusterMode.STANDALONE, "cloud-1");
        }
        
        JsonObject clusterCfg = cfg.getAsJsonObject("cluster");
        boolean enabled = clusterCfg.has("enabled") && clusterCfg.get("enabled").getAsBoolean();
        ClusterMode mode = ClusterMode.STANDALONE;
        
        if (clusterCfg.has("mode")) {
            try {
                mode = ClusterMode.valueOf(clusterCfg.get("mode").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid mode, default to STANDALONE
            }
        }
        
        String nodeId = clusterCfg.has("node_id") ? clusterCfg.get("node_id").getAsString() : "cloud-1";
        
        return new ClusterEntry(enabled, mode, nodeId);
    }
}
