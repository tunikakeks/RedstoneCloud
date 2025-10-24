package de.redstonecloud.cloud.cluster;

import com.google.gson.JsonObject;
import de.redstonecloud.cloud.config.CloudConfig;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.Collection;

@Getter
@Log4j2
public class NodeManager {
    private static volatile NodeManager INSTANCE;

    private final Object2ObjectOpenHashMap<String, ClusterNode> nodes = new Object2ObjectOpenHashMap<>();
    private final boolean clusteringEnabled;

    public static NodeManager getInstance() {
        if (INSTANCE == null) {
            synchronized (NodeManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new NodeManager();
                }
            }
        }
        return INSTANCE;
    }

    private NodeManager() {
        JsonObject cfg = CloudConfig.getCfg();
        this.clusteringEnabled = cfg.has("clustering") && 
                                 cfg.getAsJsonObject("clustering").get("enabled").getAsBoolean();
        
        if (clusteringEnabled) {
            loadNodes();
            log.info("Clustering enabled - loaded {} configured nodes", nodes.size());
        } else {
            log.info("Clustering disabled");
        }
    }

    private void loadNodes() {
        JsonObject cfg = CloudConfig.getCfg();
        JsonObject clusteringCfg = cfg.getAsJsonObject("clustering");
        JsonObject nodesObj = clusteringCfg.getAsJsonObject("nodes");
        
        if (nodesObj != null) {
            nodesObj.keySet().forEach(nodeName -> {
                String authKey = nodesObj.get(nodeName).getAsString();
                ClusterNode node = ClusterNode.builder()
                        .name(nodeName)
                        .authKey(authKey)
                        .authenticated(false)
                        .lastHeartbeat(0)
                        .build();
                nodes.put(nodeName, node);
                log.debug("Loaded node configuration: {}", nodeName);
            });
        }
    }

    public boolean authenticate(String nodeName, String authKey) {
        if (!clusteringEnabled) {
            log.warn("Node authentication attempt while clustering is disabled");
            return false;
        }

        ClusterNode node = nodes.get(nodeName);
        if (node == null) {
            log.warn("Authentication failed: Unknown node '{}'", nodeName);
            return false;
        }

        if (!node.getAuthKey().equals(authKey)) {
            log.warn("Authentication failed: Invalid auth key for node '{}'", nodeName);
            return false;
        }

        node.setAuthenticated(true);
        node.setLastHeartbeat(System.currentTimeMillis());
        log.info("Node '{}' authenticated successfully", nodeName);
        return true;
    }

    public ClusterNode getNode(String name) {
        return nodes.get(name);
    }

    public Collection<ClusterNode> getAllNodes() {
        return nodes.values();
    }

    public boolean isNodeAuthenticated(String name) {
        ClusterNode node = nodes.get(name);
        return node != null && node.isAuthenticated();
    }
}
