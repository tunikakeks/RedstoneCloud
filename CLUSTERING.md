# RedstoneCloud Clustering

RedstoneCloud now supports optional clustering with master-slave architecture.

## Overview

The clustering feature allows you to run multiple RedstoneCloud instances that share the same Redis instance for communication and data synchronization.

### Cluster Modes

1. **STANDALONE** (default) - Single instance with full functionality
2. **MASTER** - Manages server lifecycle and templates in a cluster
3. **SLAVE** - Read-only instance, no server management

## Configuration

Edit `cloud.json` to configure clustering:

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
