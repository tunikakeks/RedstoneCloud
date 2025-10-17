package de.redstonecloud.api.redis.broker.packet.defaults.cluster;

import com.google.gson.JsonArray;
import de.redstonecloud.api.redis.broker.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from slave to master to register itself and announce its templates.
 */
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class RegisterSlavePacket extends Packet {
    public static int NETWORK_ID = 100;

    protected String nodeId;
    protected List<String> templateNames = new ArrayList<>();

    @Override
    public int packetId() {
        return NETWORK_ID;
    }

    @Override
    public void serialize(JsonArray data) {
        data.add(this.nodeId);
        JsonArray templates = new JsonArray();
        for (String template : this.templateNames) {
            templates.add(template);
        }
        data.add(templates);
    }

    @Override
    public void deserialize(JsonArray data) {
        this.nodeId = data.get(0).getAsString();
        JsonArray templates = data.get(1).getAsJsonArray();
        this.templateNames = new ArrayList<>();
        for (int i = 0; i < templates.size(); i++) {
            this.templateNames.add(templates.get(i).getAsString());
        }
    }
}
