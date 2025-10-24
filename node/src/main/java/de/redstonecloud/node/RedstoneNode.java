package de.redstonecloud.node;

import de.redstonecloud.api.encryption.KeyManager;
import de.redstonecloud.api.encryption.cache.KeyCache;
import de.redstonecloud.api.redis.broker.BrokerHelper;
import de.redstonecloud.api.redis.broker.packet.defaults.cluster.NodeAuthPacket;
import de.redstonecloud.api.util.Keys;
import de.redstonecloud.node.config.NodeConfig;
import de.redstonecloud.node.redis.NodePacketHandler;
import de.redstonecloud.node.server.NodeServerManager;
import de.redstonecloud.node.utils.Directories;
import de.redstonecloud.node.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import de.redstonecloud.api.redis.broker.Broker;
import de.redstonecloud.api.redis.cache.Cache;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.security.PublicKey;

@Getter
@Log4j2
public class RedstoneNode {
    @Getter
    private static RedstoneNode instance;
    @Getter
    public static String workingDir;
    @Getter
    public static Cache cache;
    private static boolean running = false;

    @Getter
    private static Broker broker;

    @Setter
    private boolean authenticated = false;

    @Getter
    private String nodeName;

    @SneakyThrows
    public static void main(String[] args) {
        workingDir = System.getProperty("user.dir");

        if (!Directories.setupCheck.exists()) Utils.setup();

        String redisIp = NodeConfig.getRedisIp();
        String redisPort = NodeConfig.getRedisPort();
        int redisDb = NodeConfig.getRedisDb();

        System.setProperty(Keys.PROPERTY_REDIS_PORT, redisPort);
        System.setProperty(Keys.PROPERTY_REDIS_IP, redisIp);
        System.setProperty(Keys.PROPERTY_REDIS_DB, String.valueOf(redisDb));

        Thread.sleep(2000);

        cache = new Cache();

        String nodeName = NodeConfig.getNodeName();

        try {
            log.info("Connecting to Redis...");
            broker = new Broker(nodeName, BrokerHelper.constructRegistry(), nodeName, NodeConfig.getMasterChannel());
            broker.listen(nodeName, NodePacketHandler::handle);
            broker.listen(NodeConfig.getMasterChannel(), NodePacketHandler::handle);
        } catch (Exception e) {
            log.error("Redis IP: {}", redisIp);
            log.error("Redis Port: {}", redisPort);
            throw new RuntimeException("Cannot connect to Redis: " + e);
        }

        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("log4j.skipJansi", "false");
        System.setProperty("Dterminal.jline", "true");
        System.setProperty("Dterminal.ansi", "true");
        System.setProperty("Djansi.passthrough", "true");

        RedstoneNode node = new RedstoneNode();

        Runtime.getRuntime().addShutdownHook(new Thread(node::stop));
    }

    protected NodeServerManager serverManager;
    protected KeyCache keyCache;

    protected boolean stopped = false;

    public RedstoneNode() {
        instance = this;
        boot();
    }

    public void boot() {
        running = true;

        this.nodeName = NodeConfig.getNodeName();

        PublicKey publicKey = KeyManager.init();
        this.keyCache = new KeyCache();
        this.keyCache.addKey(nodeName, publicKey);

        log.info("Starting RedstoneNode: {}", nodeName);

        Utils.createBaseFolders();

        this.serverManager = NodeServerManager.getInstance();

        // Authenticate with master
        authenticateWithMaster();

        log.info("Node {} initialized and waiting for authentication", nodeName);
    }

    private void authenticateWithMaster() {
        String authKey = NodeConfig.getAuthKey();
        String masterChannel = NodeConfig.getMasterChannel();
        
        log.info("Sending authentication request to master...");
        
        new NodeAuthPacket(nodeName, authKey)
                .setTo(masterChannel)
                .send();
    }

    public void stop() {
        if (this.stopped || !running) {
            return;
        }

        this.stopped = true;
        running = false;

        try {
            Thread.sleep(200);
            log.info("Node shutdown started");
            
            boolean serversKilled = this.serverManager.stopAll();
            if (serversKilled) {
                log.info("All servers stopped");
            }
            
            Thread.sleep(500);
            log.info("Node shutdown complete");

            broker.shutdown();
        } catch (InterruptedException e) {
            log.error("Error during shutdown: ", e);
        } catch (Exception e) {
            log.error("Unexpected error during shutdown: ", e);
        }

        System.exit(0);
    }

    public static boolean isRunning() {
        return running;
    }
}
