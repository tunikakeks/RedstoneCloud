package de.redstonecloud.api.redis.broker.packet.defaults.cluster;

import com.google.gson.JsonArray;
import de.redstonecloud.api.redis.broker.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Packet sent from master to slave to request current template list.
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class RequestSlaveTemplatesPacket extends Packet {
    public static int NETWORK_ID = 102;

    protected String targetNodeId;

    @Override
    public int packetId() {
        return NETWORK_ID;
    }

    @Override
    public void serialize(JsonArray data) {
        data.add(this.targetNodeId);
    }

    @Override
    public void deserialize(JsonArray data) {
        this.targetNodeId = data.get(0).getAsString();
    }
}
