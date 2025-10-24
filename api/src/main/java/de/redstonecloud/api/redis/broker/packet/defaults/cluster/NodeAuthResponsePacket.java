package de.redstonecloud.api.redis.broker.packet.defaults.cluster;

import com.google.gson.JsonArray;
import de.redstonecloud.api.redis.broker.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class NodeAuthResponsePacket extends Packet {
    public static int NETWORK_ID = 101;

    protected boolean success;
    protected String message;

    @Override
    public int packetId() {
        return NETWORK_ID;
    }

    @Override
    public void serialize(JsonArray data) {
        data.add(this.success);
        data.add(this.message);
    }

    @Override
    public void deserialize(JsonArray data) {
        this.success = data.get(0).getAsBoolean();
        this.message = data.get(1).getAsString();
    }
}
