package de.redstonecloud.cloud.cluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterNode {
    private String name;
    private String authKey;
    private boolean authenticated;
    private long lastHeartbeat;
}
