package de.redstonecloud.cloud.cluster;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ClusterNode {
    private String name;
    private String authKey;
    private boolean connected;
    private long lastHeartbeat;

    public ClusterNode(String name, String authKey) {
        this.name = name;
        this.authKey = authKey;
        this.connected = false;
        this.lastHeartbeat = System.currentTimeMillis();
    }
}
