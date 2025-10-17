package de.redstonecloud.api.redis.broker.packet.defaults.cluster;

import com.google.gson.JsonArray;
import de.redstonecloud.api.redis.broker.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Packet sent from slave to master when template configuration changes.
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class SlaveTemplateUpdatePacket extends Packet {
    public static int NETWORK_ID = 101;

    protected String nodeId;
    protected String templateName;
    protected String action; // "add", "remove", "update"

    @Override
    public int packetId() {
        return NETWORK_ID;
    }

    @Override
    public void serialize(JsonArray data) {
        data.add(this.nodeId);
        data.add(this.templateName);
        data.add(this.action);
    }

    @Override
    public void deserialize(JsonArray data) {
        this.nodeId = data.get(0).getAsString();
        this.templateName = data.get(1).getAsString();
        this.action = data.get(2).getAsString();
    }
}
