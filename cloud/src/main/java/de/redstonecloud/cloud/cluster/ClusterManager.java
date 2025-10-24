package de.redstonecloud.cloud.cluster;

import de.redstonecloud.api.redis.broker.packet.defaults.cluster.NodeStopPacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.Collection;

@Getter
@Log4j2
public class ClusterManager {
    private static ClusterManager INSTANCE;

    private final Object2ObjectOpenHashMap<String, ClusterNode> nodes = new Object2ObjectOpenHashMap<>();
    private boolean clusteringEnabled = false;

    public static ClusterManager getInstance() {
        return INSTANCE != null ? INSTANCE : new ClusterManager();
    }

    private ClusterManager() {
        INSTANCE = this;
    }

    public void setClusteringEnabled(boolean enabled) {
        this.clusteringEnabled = enabled;
    }

    public void addNode(String name, String authKey) {
        nodes.put(name, new ClusterNode(name, authKey));
        log.info("Added cluster node: {}", name);
    }

    public void removeNode(String name) {
        nodes.remove(name);
        log.info("Removed cluster node: {}", name);
    }

    public ClusterNode getNode(String name) {
        return nodes.get(name);
    }

    public boolean authenticateNode(String name, String authKey) {
        ClusterNode node = nodes.get(name);
        if (node == null) {
            log.warn("Node authentication failed: node {} not found", name);
            return false;
        }

        if (!node.getAuthKey().equals(authKey)) {
            log.warn("Node authentication failed: invalid auth key for node {}", name);
            return false;
        }

        node.setConnected(true);
        node.setLastHeartbeat(System.currentTimeMillis());
        log.info("Node {} authenticated successfully", name);
        return true;
    }

    public void stopNode(String name) {
        ClusterNode node = nodes.get(name);
        if (node == null) {
            log.warn("Cannot stop node {}: not found", name);
            return;
        }

        new NodeStopPacket(name)
                .setTo(name)
                .send();

        node.setConnected(false);
        log.info("Sent stop command to node {}", name);
    }

    public Collection<ClusterNode> getAllNodes() {
        return nodes.values();
    }

    public int getConnectedNodeCount() {
        return (int) nodes.values().stream().filter(ClusterNode::isConnected).count();
    }
}
