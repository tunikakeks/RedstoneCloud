# RedstoneCloud Configuration Examples

This directory contains example configuration files for different deployment scenarios.

## Files

### cloud.json.standalone
Default configuration for a single-instance deployment. This is the traditional setup without clustering.

**Usage:**
```bash
cp cloud.json.standalone cloud.json
```

**Features:**
- Single instance with full functionality
- Starts internal Redis server
- Manages all servers and templates
- Backwards compatible with previous versions

---

### cloud.json.master
Configuration for the master node in a clustered deployment.

**Usage:**
```bash
cp cloud.json.master cloud.json
```

**Features:**
- Manages all server lifecycle operations
- Starts internal Redis server (bound to 0.0.0.0 for network access)
- Full administrative capabilities
- Only ONE master should run in a cluster

**Important Notes:**
- Set `redis_bind` to `0.0.0.0` or a specific network interface if slave nodes need to connect from other machines
- Update `redis_bind` to `127.0.0.1` if all nodes are on the same machine
- Ensure the Redis port (6379) is accessible from slave nodes

---

### cloud.json.slave
Configuration for slave nodes in a clustered deployment.

**Usage:**
```bash
cp cloud.json.slave cloud.json
# Edit cloud.json and update redis_bind to point to the master's IP address
```

**Features:**
- Read-only access to server data
- Cannot create or manage servers
- Multiple slaves can run simultaneously
- Each slave should have a unique `node_id`

**Important Notes:**
- Set `custom_redis` to `true` (slaves don't start their own Redis)
- Update `redis_bind` to the IP address of the master node
- Change `node_id` to a unique value (e.g., cloud-slave-1, cloud-slave-2)

## Deployment Scenarios

### Scenario 1: Single Server (Default)
Use `cloud.json.standalone` - no changes needed.

### Scenario 2: Cluster on Same Machine
1. Master: Use `cloud.json.master` with `redis_bind: "127.0.0.1"`
2. Slaves: Use `cloud.json.slave` with `redis_bind: "127.0.0.1"` and unique `node_id`

### Scenario 3: Cluster Across Multiple Machines
1. Master: Use `cloud.json.master` with `redis_bind: "0.0.0.0"` or specific network interface
2. Slaves: Use `cloud.json.slave` with `redis_bind` set to master's IP address and unique `node_id`

## Security Considerations

When running a cluster across multiple machines:
- Consider using Redis authentication (password)
- Use a firewall to restrict Redis port access
- Use a VPN or private network for Redis communication
- Don't expose Redis to the public internet

## Troubleshooting

**Slave can't connect to Redis:**
- Check that `redis_bind` in slave config points to the master's IP
- Verify the Redis port is not blocked by a firewall
- Ensure the master is running and Redis is started

**Multiple masters running:**
- Only ONE master should run in a cluster
- Having multiple masters will cause conflicts

**Slave trying to manage servers:**
- Verify `cluster.mode` is set to `SLAVE`
- Check logs for any error messages
