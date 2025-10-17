package de.redstonecloud.cloud.config.entry;

/**
 * Defines the cluster mode for the cloud instance.
 */
public enum ClusterMode {
    /**
     * Standalone mode - single instance with full functionality (default).
     */
    STANDALONE,
    
    /**
     * Master mode - manages server lifecycle and templates in a cluster.
     */
    MASTER,
    
    /**
     * Slave mode - read-only instance in a cluster, no server management.
     */
    SLAVE
}
