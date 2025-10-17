# RedstoneCloud Clustering

RedstoneCloud now supports optional clustering with master-slave architecture.

## Overview

The clustering feature allows you to run multiple RedstoneCloud instances that share the same Redis instance for communication and data synchronization.

### Cluster Modes

1. **STANDALONE** (default) - Single instance with full functionality
2. **MASTER** - Manages server lifecycle and templates in a cluster
3. **SLAVE** - Read-only instance, no server management

## Configuration

Edit `cloud.json` to configure clustering. See the `examples/` directory for ready-to-use configuration templates.

```json
{
  "cluster": {
    "enabled": false,
    "mode": "STANDALONE",
    "node_id": "cloud-1"
  }
}
```

### Configuration Options

- **enabled**: Set to `true` to enable clustering (default: `false`)
- **mode**: Cluster mode - `STANDALONE`, `MASTER`, or `SLAVE` (default: `STANDALONE`)
- **node_id**: Unique identifier for this cluster node (default: `cloud-1`)

## Usage Examples

For ready-to-use configuration files, see the `examples/` directory. Below are the configuration details for each mode.

### Standalone Mode (Default)

This is the default mode and works exactly as before. No cluster configuration needed.

```json
{
  "cluster": {
    "enabled": false,
    "mode": "STANDALONE",
    "node_id": "cloud-1"
  }
}
```

### Master Node

The master node manages all server lifecycle operations, starts/stops servers, and manages templates. Only ONE master should be running in a cluster.

```json
{
  "redis_port": 6379,
  "redis_bind": "127.0.0.1",
  "redis_db": 0,
  "custom_redis": false,
  "cluster": {
    "enabled": true,
    "mode": "MASTER",
    "node_id": "cloud-master"
  }
}
```

**Features:**
- Manages server lifecycle (start/stop servers)
- Template management
- Can start internal Redis instance if `custom_redis` is `false`
- Full administrative capabilities
- **Discovers and queries templates from all slave nodes**
- **Can start servers using slave templates** - just use `start <template>` with any slave template name
- Use `cluster` command to view slave templates and node information

### Slave Node

Slave nodes connect to the same Redis instance and have read-only access. They can view server status but cannot create or manage servers.

```json
{
  "redis_port": 6379,
  "redis_bind": "127.0.0.1",
  "redis_db": 0,
  "custom_redis": true,
  "cluster": {
    "enabled": true,
    "mode": "SLAVE",
    "node_id": "cloud-slave-1"
  }
}
```

**Note:** When running slave nodes, set `custom_redis` to `true` and point to the same Redis instance as the master.

**Features:**
- Read-only access to server data
- Cannot create or stop servers
- Shares Redis with master for synchronization
- **Each slave stores its own templates AND server types in Redis**
  - Templates: `template:{nodeId}:{templateName}`
  - Types: `type:{nodeId}:{typeName}`
- Automatically registers with master on startup
- Master can discover and query slave templates
- **Master can start servers using slave templates**
- Multiple slave instances can run simultaneously
- Types with same names on different nodes don't conflict
- Multiple slave instances can run simultaneously

## Architecture

```
┌─────────────────┐
│  Master Node    │
│  (cloud-master) │
│                 │
│  - Starts Redis │
│  - Manages      │
│    Servers      │
└────────┬────────┘
         │
         ├─── Redis (Shared) ───┐
         │                      │
┌────────▼────────┐    ┌────────▼────────┐
│  Slave Node 1   │    │  Slave Node 2   │
│ (cloud-slave-1) │    │ (cloud-slave-2) │
│                 │    │                 │
│  - Read-only    │    │  - Read-only    │
│  - No server    │    │  - No server    │
│    management   │    │    management   │
└─────────────────┘    └─────────────────┘
```

## Important Notes

1. **Only ONE master** should be running in a cluster at any time
2. All nodes must connect to the **same Redis instance**
3. The master node should start the internal Redis or point to an external one
4. Slave nodes should always set `custom_redis` to `true`
5. Each node should have a **unique node_id**
6. Clustering is **optional** and disabled by default (backwards compatible)

## Migration from Non-Clustered Setup

If you're upgrading from a non-clustered setup, no changes are needed. The default configuration remains:

```json
{
  "cluster": {
    "enabled": false,
    "mode": "STANDALONE",
    "node_id": "cloud-1"
  }
}
```

This maintains full backwards compatibility.

## Quick Start

See `examples/README.md` for:
- Ready-to-use configuration files
- Deployment scenarios (single server, same-machine cluster, multi-machine cluster)
- Security considerations
- Troubleshooting tips

## Cluster Command (Master Only)

The master node can use the `cluster` command to view information about slave nodes and their templates:

```bash
# List all registered slave nodes
cluster list

# View templates for a specific slave node
cluster templates cloud-slave-1

# View all templates from all slave nodes
cluster all
```

Example output:
```
Registered Slave Nodes:
+-----------------------+------------+
| Node ID               | Templates  |
+-----------------------+------------+
| cloud-slave-1         | 3          |
| cloud-slave-2         | 2          |
+-----------------------+------------+
```

## Starting Servers from Slave Templates (Master Only)

The master can start servers using templates from any slave node. Simply use the `start` command with the template name:

```bash
# Start a server from a slave template
start SlaveTemplateName

# Start multiple servers
start SlaveTemplateName --amount 3

# Start with specific ID
start SlaveTemplateName --id 5
```

The master will:
1. Automatically detect if the template is from a slave node
2. Load the template and its server type from Redis
3. Create and manage the server just like a local template

**Note:** The server is created and managed by the master, not the slave. The slave only provides the template configuration.

## Template Storage

In cluster mode, each slave stores its templates AND server types in Redis with the following key patterns:
```
template:{nodeId}:{templateName}
type:{nodeId}:{typeName}
```

For example:
- `template:cloud-slave-1:Lobby`
- `type:cloud-slave-1:Proxy`
- `template:cloud-slave-2:GameServer`
- `type:cloud-slave-2:Server`

This allows the master to discover and query templates from all slave nodes dynamically, and ensures that server types with the same name on different nodes don't conflict. Slaves automatically register their templates with the master on startup.

**Master can start servers from slave templates:** The master node can use the `start` command with any template from any slave node. The server will be created and managed by the master, even if the template comes from a slave.
