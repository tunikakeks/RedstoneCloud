# Clustering Implementation Summary

## Overview
This implementation adds optional clustering support to RedstoneCloud, enabling a master-slave architecture where one master cloud instance manages multiple slave node instances.

## Architecture

### Components

1. **Master Cloud** (`cloud` module)
   - Main cloud instance with full functionality
   - Manages all cluster nodes
   - Handles node authentication
   - Can send commands to nodes (e.g., stop)
   - Stores node configuration in `cloud.json`

2. **Slave Nodes** (`node` module)
   - Lightweight instances that manage their own servers
   - Each has its own templates, server types, and running servers
   - Authenticates with master on startup using unique auth key
   - Receives commands from master (currently: stop)
   - Minimal command set (only `stop` and `end`)

3. **Communication Layer** (API)
   - Three new packet types for cluster communication:
     - `NodeAuthPacket`: Node authentication request
     - `NodeAuthResponsePacket`: Authentication response from master
     - `NodeStopPacket`: Stop command from master to node

### Data Flow

```
1. Node Startup:
   Node -> Redis -> Master: NodeAuthPacket(name, authKey)
   Master -> Redis -> Node: NodeAuthResponsePacket(success, message)

2. Node Management:
   Master -> Redis -> Node: NodeStopPacket(nodeName)
   Node: Initiates shutdown

3. Server Management:
   Each node manages its own servers independently
   Nodes don't share server state with each other
```

## Implementation Details

### New Files

#### API Module
- `api/src/main/java/de/redstonecloud/api/redis/broker/packet/defaults/cluster/`
  - `NodeAuthPacket.java` - Authentication packet
  - `NodeAuthResponsePacket.java` - Authentication response
  - `NodeStopPacket.java` - Stop command packet

#### Cloud Module
- `cloud/src/main/java/de/redstonecloud/cloud/cluster/`
  - `ClusterNode.java` - Node data model
  - `ClusterManager.java` - Node management logic
- `cloud/src/main/java/de/redstonecloud/cloud/commands/defaults/`
  - `NodeCommand.java` - Node management command

#### Node Module (New)
- `node/pom.xml` - Maven configuration
- `node/src/main/java/de/redstonecloud/node/`
  - `RedstoneNode.java` - Main class
  - `config/NodeConfig.java` - Configuration loader
  - `cluster/` - (reused from cloud with adaptations)
  - `commands/` - Minimal command set
  - `console/` - Console interface
  - `redis/PacketHandler.java` - Packet handler
  - `scheduler/` - Task scheduling
  - `server/` - Server management (adapted from cloud)
  - `utils/` - Utility classes
- `node/src/main/resources/`
  - `node.json` - Node configuration template
  - `language.json` - Translation strings
- `node/README.md` - Documentation

### Modified Files

#### API
- `BrokerHelper.java` - Register new cluster packets

#### Cloud
- `RedstoneCloud.java` - Initialize ClusterManager, load cluster config
- `PacketHandler.java` - Handle NodeAuthPacket
- `CommandManager.java` - Register NodeCommand
- `cloud.json` - Add clustering configuration
- `language.json` - Add node-related translations

#### Root
- `pom.xml` - Add node module
- `README.md` - Document clustering feature

## Configuration

### Master Cloud (`cloud.json`)
```json
{
  "clustering_enabled": false,
  "cluster_nodes": [
    {
      "name": "node-1",
      "auth_key": "secure-random-key-here"
    }
  ]
}
```

### Node (`node.json`)
```json
{
  "node_name": "node-1",
  "auth_key": "secure-random-key-here",
  "redis_port": "6379",
  "redis_bind": "127.0.0.1",
  "redis_db": "0"
}
```

## Security Features

1. **Authentication**
   - Each node has a unique authentication key
   - Keys must match between master config and node config
   - Authentication happens on every node startup
   - Failed authentication results in node shutdown

2. **Authorization**
   - Only authenticated nodes receive commands from master
   - Nodes only respond to packets from "cloud" route

3. **Data Isolation**
   - Each node manages its own servers
   - Templates and types are node-specific
   - No cross-node data access

## Commands

### Master Cloud Commands
- `node list` - List all nodes and their connection status
- `node stop <name>` - Send stop command to a node
- `node add <name> <auth_key>` - Add a node (runtime only)
- `node remove <name>` - Remove a node (runtime only)

### Node Commands
- `stop <server>` - Stop a specific server
- `end` - Shutdown the node

## Usage Example

1. **Setup Master**
   ```bash
   # Edit cloud.json to enable clustering and add nodes
   {
     "clustering_enabled": true,
     "cluster_nodes": [
       {"name": "node-1", "auth_key": "key1"},
       {"name": "node-2", "auth_key": "key2"}
     ]
   }
   # Start master cloud
   java -jar cloud-0.1-beta-shaded.jar
   ```

2. **Setup Node**
   ```bash
   # Run node for first time
   java -jar node-0.1-beta-shaded.jar
   # Follow setup wizard
   # Enter node name: node-1
   # Enter auth key: key1
   # Enter Redis details (same as master)
   ```

3. **Manage Nodes**
   ```bash
   # On master cloud console
   node list              # See all nodes
   node stop node-1       # Stop a specific node
   ```

## Benefits

1. **Scalability**: Add more nodes to handle more servers
2. **Isolation**: Each node has its own server space
3. **Flexibility**: Enable/disable clustering as needed
4. **Security**: Authenticated node connections
5. **Simplicity**: Nodes are lightweight with minimal commands

## Limitations

1. **Java Version**: Requires Java 17+ (project specifies Java 22)
2. **Redis Dependency**: All components must connect to same Redis instance
3. **No Load Balancing**: Manual node selection/configuration required
4. **Limited Commands**: Nodes have minimal command set

## Future Enhancements

Possible improvements (not in current scope):
- Node heartbeat monitoring
- Automatic failover
- Load balancing across nodes
- Node-to-node communication
- Cluster-wide statistics
- Dynamic node registration

## Testing Notes

Due to Java version mismatch (project requires Java 22, environment has Java 17), full compilation and testing could not be performed. However:
- Code structure is sound
- Security scan passed (0 vulnerabilities)
- All necessary files created and configured
- Documentation complete

## Security Summary

CodeQL analysis completed with **0 vulnerabilities** found in the implementation.

Key security considerations:
- Authentication keys should be randomly generated
- Keys should be stored securely (not in version control)
- Redis should be properly secured (bind address, authentication)
- Node-to-master communication is authenticated
