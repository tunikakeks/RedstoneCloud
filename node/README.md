# RedstoneCloud Node Module

This module provides clustering support for RedstoneCloud, allowing you to run multiple slave nodes managed by a single master cloud instance.

## Overview

The clustering architecture consists of:
- **Master Cloud**: The main cloud instance that manages all cluster nodes
- **Slave Nodes**: Independent node instances that run their own servers and templates

Each node has:
- Its own templates, server types, and running servers
- A unique name and authentication key
- Independent server management
- Connection to the same Redis instance as the master

## Configuration

### Master Cloud Setup

1. Edit `cloud.json` to enable clustering:
```json
{
  "clustering_enabled": true,
  "cluster_nodes": [
    {
      "name": "node-1",
      "auth_key": "your-secure-auth-key-here"
    },
    {
      "name": "node-2",
      "auth_key": "another-secure-auth-key"
    }
  ]
}
```

2. Start the master cloud as usual

### Node Setup

1. Run the node JAR for the first time to start setup:
```bash
java -jar node-0.1-beta-shaded.jar
```

2. During setup, provide:
   - Node name (must match the name in master cloud config)
   - Auth key (must match the key in master cloud config)
   - Redis connection details (same Redis instance as master)

3. The node will authenticate with the master cloud on startup

## Management Commands

### Master Cloud Commands

Use the `node` command to manage cluster nodes:

- `node list` - List all configured nodes and their status
- `node stop <name>` - Send stop command to a specific node
- `node add <name> <auth_key>` - Add a new node (must also update cloud.json)
- `node remove <name>` - Remove a node (must also update cloud.json)

### Node Commands

Nodes have limited commands:
- `stop <server>` - Stop a specific server running on this node
- `end` - Shutdown the node

## How It Works

1. Each node authenticates with the master cloud using its unique auth key
2. Nodes manage their own servers independently
3. All communication happens through Redis pub/sub
4. The master cloud can send commands to nodes (e.g., stop command)
5. Nodes report server status changes back to the master

## Security

- Each node must have a unique, secure authentication key
- Keys should be randomly generated and kept secret
- Only authenticated nodes can connect to the cluster
- Authentication happens on every node startup

## Example Usage

1. Setup master cloud with clustering enabled
2. Start master cloud
3. Setup and start node-1
4. Node-1 authenticates with master
5. Node-1 starts managing its own servers
6. Use `node list` on master to see node-1 connected
7. Use `node stop node-1` on master to shutdown node-1
