package de.redstonecloud.cloud.config.entry;

/**
 * Configuration entry for cluster settings.
 * 
 * @param enabled whether clustering is enabled
 * @param mode the cluster mode (STANDALONE, MASTER, or SLAVE)
 * @param nodeId unique identifier for this cluster node
 */
public record ClusterEntry(boolean enabled, ClusterMode mode, String nodeId) {
}
