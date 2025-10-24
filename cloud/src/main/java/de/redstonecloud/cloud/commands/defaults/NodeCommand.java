package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.cluster.ClusterManager;
import de.redstonecloud.cloud.cluster.ClusterNode;
import de.redstonecloud.cloud.commands.Command;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class NodeCommand extends Command {

    public NodeCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        ClusterManager clusterManager = RedstoneCloud.getInstance().getClusterManager();

        if (!clusterManager.isClusteringEnabled()) {
            log.error("Clustering is not enabled. Enable it in cloud.json");
            return;
        }

        if (args.length == 0) {
            showUsage();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> listNodes(clusterManager);
            case "stop" -> stopNode(args, clusterManager);
            case "add" -> addNode(args, clusterManager);
            case "remove" -> removeNode(args, clusterManager);
            default -> showUsage();
        }
    }

    private void showUsage() {
        log.info("Usage: node <list|stop|add|remove> [args]");
        log.info("  node list - List all cluster nodes");
        log.info("  node stop <name> - Stop a cluster node");
        log.info("  node add <name> <auth_key> - Add a cluster node");
        log.info("  node remove <name> - Remove a cluster node");
    }

    private void listNodes(ClusterManager clusterManager) {
        var nodes = clusterManager.getAllNodes();
        if (nodes.isEmpty()) {
            log.info("No cluster nodes configured");
            return;
        }

        log.info("Cluster Nodes ({} total, {} connected):", nodes.size(), clusterManager.getConnectedNodeCount());
        for (ClusterNode node : nodes) {
            log.info("  - {} [{}]", node.getName(), node.isConnected() ? "CONNECTED" : "DISCONNECTED");
        }
    }

    private void stopNode(String[] args, ClusterManager clusterManager) {
        if (args.length < 2) {
            log.error("Usage: node stop <name>");
            return;
        }

        String nodeName = args[1];
        ClusterNode node = clusterManager.getNode(nodeName);
        
        if (node == null) {
            log.error("Node {} not found", nodeName);
            return;
        }

        clusterManager.stopNode(nodeName);
        log.info("Sent stop command to node {}", nodeName);
    }

    private void addNode(String[] args, ClusterManager clusterManager) {
        if (args.length < 3) {
            log.error("Usage: node add <name> <auth_key>");
            return;
        }

        String nodeName = args[1];
        String authKey = args[2];

        clusterManager.addNode(nodeName, authKey);
        log.info("Added node {} (don't forget to update cloud.json)", nodeName);
    }

    private void removeNode(String[] args, ClusterManager clusterManager) {
        if (args.length < 2) {
            log.error("Usage: node remove <name>");
            return;
        }

        String nodeName = args[1];
        clusterManager.removeNode(nodeName);
        log.info("Removed node {} (don't forget to update cloud.json)", nodeName);
    }

    @Override
    public String[] getArgs() {
        return new String[]{"list", "stop", "add", "remove"};
    }
}
