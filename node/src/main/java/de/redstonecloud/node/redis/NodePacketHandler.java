package de.redstonecloud.node.redis;

import de.redstonecloud.api.redis.broker.packet.Packet;
import de.redstonecloud.api.redis.broker.packet.defaults.cluster.NodeAuthResponsePacket;
import de.redstonecloud.api.redis.broker.packet.defaults.cluster.NodeStopPacket;
import de.redstonecloud.node.RedstoneNode;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class NodePacketHandler {

    public static void handle(Packet packet) {
        switch (packet) {
            case NodeAuthResponsePacket pk -> on(pk);
            case NodeStopPacket pk -> on(pk);
            default -> {
            }
        }
    }

    private static void on(NodeAuthResponsePacket packet) {
        if (packet.isSuccess()) {
            log.info("Node authenticated successfully: {}", packet.getMessage());
            RedstoneNode.getInstance().setAuthenticated(true);
        } else {
            log.error("Node authentication failed: {}", packet.getMessage());
            log.error("Shutting down...");
            System.exit(1);
        }
    }

    private static void on(NodeStopPacket packet) {
        String nodeName = RedstoneNode.getInstance().getNodeName();
        if (packet.getNodeName().equals(nodeName)) {
            log.info("Received stop command from master");
            RedstoneNode.getInstance().stop();
        }
    }
}
