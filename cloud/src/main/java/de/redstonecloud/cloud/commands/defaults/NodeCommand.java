package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.api.redis.broker.packet.defaults.cluster.NodeStopPacket;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.cluster.ClusterNode;
import de.redstonecloud.cloud.cluster.NodeManager;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class NodeCommand extends Command {

    public NodeCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (args.length == 0) {
            log.info("Usage: node <list|stop> [node-name]");
            return;
        }

        String action = args[0].toLowerCase();
        NodeManager nodeManager = RedstoneCloud.getInstance().getNodeManager();

        switch (action) {
            case "list" -> {
                log.info("Registered nodes:");
                for (ClusterNode node : nodeManager.getAllNodes()) {
                    String status = node.isAuthenticated() ? "authenticated" : "not authenticated";
                    log.info("  - {} ({})", node.getName(), status);
                }
            }
            case "stop" -> {
                if (args.length < 2) {
                    log.error("Usage: node stop <node-name>");
                    return;
                }
                
                String nodeName = args[1];
                ClusterNode node = nodeManager.getNode(nodeName);
                
                if (node == null) {
                    log.error("Node '{}' not found", nodeName);
                    return;
                }
                
                if (!node.isAuthenticated()) {
                    log.warn("Node '{}' is not authenticated", nodeName);
                }
                
                new NodeStopPacket(nodeName)
                        .setTo(nodeName)
                        .send();
                
                log.info("Stop command sent to node '{}'", nodeName);
            }
            default -> log.error("Unknown action: {}. Use 'list' or 'stop'", action);
        }
    }

    @Override
    public String[] getArgs() {
        return new String[]{"list", "stop"};
    }
}
