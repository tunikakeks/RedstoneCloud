# Clustering Implementation Summary

## Overview
This implementation adds optional clustering support to RedstoneCloud with a master-slave architecture. The master (cloud module) manages cluster nodes while slaves (node module) handle server execution.

## Changes Made

### 1. New API Packets (api module)
Created three new packets for cluster communication:
- `NodeAuthPacket`: Sent by nodes to authenticate with the master
- `NodeAuthResponsePacket`: Response from master indicating auth success/failure
- `NodeStopPacket`: Command from master to stop a node

Location: `api/src/main/java/de/redstonecloud/api/redis/broker/packet/defaults/cluster/`

### 2. Updated Packet Registry
Modified `BrokerHelper.java` to register the new cluster packets with IDs 100-102.

### 3. Cloud Module Enhancements

#### Configuration
- Updated `cloud.json` to include clustering configuration:
  - `clustering.enabled`: Boolean to enable/disable clustering
  - `clustering.nodes`: Map of node names to authentication keys

#### Cluster Management
- `ClusterNode.java`: Data class representing a cluster node
- `NodeManager.java`: Singleton manager for handling node registration and authentication
  - Loads node configurations from cloud.json
  - Authenticates nodes using their auth keys
  - Tracks authentication status

#### Packet Handling
- Updated `PacketHandler.java` to handle:
  - `NodeAuthPacket`: Authenticates nodes and responds
  - `NodeStopPacket`: Logs stop commands (nodes handle their own shutdown)

#### Commands
- `NodeCommand.java`: New command for managing nodes
  - `node list`: Lists all registered nodes and their status
  - `node stop <name>`: Sends stop command to a specific node

#### Integration
- Updated `RedstoneCloud.java` to initialize NodeManager during boot

### 4. New Node Module

Created a complete new module (`node`) that mirrors the cloud module's server management capabilities:

#### Core Components
- `RedstoneNode.java`: Main class for the node
  - Connects to Redis
  - Authenticates with master on startup
  - Manages server lifecycle
  - Handles shutdown gracefully

#### Configuration
- `NodeConfig.java`: Configuration loader for node.json
- `node.json`: Template configuration file with:
  - Node name
  - Authentication key
  - Redis connection settings
  - Master channel name

#### Server Management
- `NodeServerManager.java`: Simplified version of ServerManager
  - Loads templates and server types
  - Manages server instances
  - No event system (simplified for node)
- `Server.java`: Server instance management (adapted from cloud)
- `Template.java`: Template configuration
- `ServerType.java`: Server type definition
- `ServerOutReader.java`: Server output logging

#### Packet Handling
- `NodePacketHandler.java`: Handles cluster packets
  - `NodeAuthResponsePacket`: Processes authentication response
  - `NodeStopPacket`: Initiates graceful shutdown

#### Utilities
- `Directories.java`: File system structure
- `Utils.java`: Basic setup utilities

### 5. Build Configuration
- Updated parent `pom.xml` to include the node module
- Created `node/pom.xml` with similar dependencies to cloud module

## Architecture

### Communication Flow
```
Master (Cloud)                    Node (Slave)
     |                                |
     |<------- NodeAuthPacket --------|  (on startup)
     |                                |
     |---- NodeAuthResponsePacket --->|  (auth result)
     |                                |
     |---- NodeStopPacket ----------->|  (when stopping)
     |                                |
```

### Key Design Decisions

1. **Optional Clustering**: Disabled by default, enabled via configuration
2. **Shared Redis**: All nodes use the same Redis instance for communication
3. **Independent Management**: Each node manages its own templates, types, and servers
4. **Centralized Control**: Master can monitor and control nodes
5. **Secure Authentication**: Each node requires a unique auth key
6. **Simplified Node**: No events, plugins, or scheduler in nodes (focused on server management)

## Testing Status

The implementation is complete but needs testing with Java 22. The build currently fails due to Java version mismatch (requires Java 22, but Java 17 is available in the environment).

## Security Features

1. Authentication required for all nodes
2. Unique auth keys per node
3. Authentication happens on connection
4. Failed authentication causes node to shut down

## Next Steps

To use this feature:
1. Build the project with Java 22
2. Configure clustering in cloud.json (optional)
3. Create node.json for each slave node
4. Start master first, then nodes
5. Use `node list` and `node stop` commands to manage cluster

## Limitations

1. No automatic node discovery
2. Nodes must be pre-configured in master
3. No heartbeat mechanism (could be added later)
4. No automatic failover
5. Node stop is one-way (no restart command)
