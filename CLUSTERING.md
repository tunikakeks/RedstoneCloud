# Clustering Feature

RedstoneCloud now supports clustering with a master-slave architecture. This allows you to distribute server loads across multiple nodes while maintaining centralized management.

## Architecture

- **Master (Cloud)**: The main cloud instance that manages all cluster nodes
- **Slave (Node)**: Worker nodes that run game servers and report to the master

## Configuration

### Master Configuration (cloud.json)

Add the clustering configuration to your `cloud.json`:

```json
{
  "redis_port": 6379,
  "redis_bind": "127.0.0.1",
  "redis_db": 0,
  "custom_redis": false,
  "clustering": {
    "enabled": true,
    "nodes": {
      "node-1": "secure-auth-key-1",
      "node-2": "secure-auth-key-2"
    }
  },
  "bridge": {
    "hub_template": "Lobby",
    "hubcommand_desc": "Go to the Lobby",
    "hubcommand_no_hub_available": "There is no hub available at the moment."
  }
}
```

- `enabled`: Set to `true` to enable clustering, `false` to disable
- `nodes`: Map of node names to their authentication keys

### Node Configuration (node.json)

Create a `node.json` configuration file for each node:

```json
{
  "node_name": "node-1",
  "auth_key": "secure-auth-key-1",
  "redis_port": 6379,
  "redis_bind": "127.0.0.1",
  "redis_db": 0,
  "master_channel": "cloud"
}
```

- `node_name`: Unique name for this node (must match the name in master config)
- `auth_key`: Authentication key (must match the key in master config)
- `redis_port`, `redis_bind`, `redis_db`: Redis connection settings (should match master)
- `master_channel`: Channel name of the master (default: "cloud")

## Usage

### Starting the Master

```bash
java -jar cloud-0.1-beta-shaded.jar
```

### Starting a Node

```bash
java -jar node-0.1-beta-shaded.jar
```

The node will automatically:
1. Connect to Redis
2. Authenticate with the master
3. Start managing servers based on its local templates

### Master Commands

#### List Nodes
```
node list
```
Shows all registered nodes and their authentication status.

#### Stop a Node
```
node stop <node-name>
```
Sends a stop command to the specified node, which will gracefully shut down all its servers and terminate.

## Features

- **Optional Clustering**: Can be enabled/disabled via configuration
- **Secure Authentication**: Each node requires a unique authentication key
- **Independent Server Management**: Each node manages its own templates, server types, and running servers
- **Centralized Control**: Master can monitor and control all nodes
- **Shared Redis Instance**: All nodes use the same Redis instance for communication

## Node Architecture

Each node:
- Has its own set of templates (in `template_configs/`)
- Has its own server types (in `types/`)
- Manages its own running servers (in `servers/`)
- Communicates with the master via Redis pub/sub
- Can be stopped remotely from the master

## Security Considerations

- Use strong, unique authentication keys for each node
- Keep authentication keys secure and don't commit them to version control
- Use a dedicated Redis instance or database for production deployments
- Consider using Redis authentication and SSL/TLS for production

## Troubleshooting

### Node fails to authenticate
- Check that the node name in `node.json` matches the name in `cloud.json`
- Verify the authentication key matches exactly
- Ensure clustering is enabled in the master configuration
- Check Redis connectivity

### Node doesn't receive stop commands
- Verify the node is authenticated (check master logs)
- Check that Redis pub/sub is working correctly
- Ensure the node name is correct in the stop command
