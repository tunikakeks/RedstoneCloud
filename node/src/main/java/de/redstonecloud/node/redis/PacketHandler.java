package de.redstonecloud.node.redis;

import de.redstonecloud.api.redis.broker.packet.Packet;
import de.redstonecloud.api.redis.broker.packet.defaults.cluster.NodeStopPacket;
import de.redstonecloud.node.RedstoneNode;
import de.redstonecloud.node.utils.Translator;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PacketHandler {

    public static void handle(Packet packet) {
        switch (packet) {
            case NodeStopPacket pk -> on(pk);
            default -> {
            }
        }
    }

    private static void on(NodeStopPacket packet) {
        log.info(Translator.translate("node.stop.requested"));
        RedstoneNode.getInstance().stop();
    }
}
