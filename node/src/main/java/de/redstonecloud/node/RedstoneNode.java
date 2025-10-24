package de.redstonecloud.node;

import de.redstonecloud.api.encryption.KeyManager;
import de.redstonecloud.api.encryption.cache.KeyCache;
import de.redstonecloud.api.redis.broker.BrokerHelper;
import de.redstonecloud.node.config.NodeConfig;
import de.redstonecloud.node.redis.PacketHandler;
import de.redstonecloud.node.server.ServerLogger;
import de.redstonecloud.node.commands.CommandManager;
import de.redstonecloud.node.console.Console;
import de.redstonecloud.node.scheduler.TaskScheduler;
import de.redstonecloud.node.scheduler.defaults.CheckTemplateTask;
import de.redstonecloud.node.server.ServerManager;
import de.redstonecloud.node.utils.Translator;
import de.redstonecloud.node.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import de.redstonecloud.api.redis.broker.Broker;
import de.redstonecloud.api.redis.cache.Cache;
import de.redstonecloud.api.redis.broker.packet.defaults.cluster.NodeAuthPacket;
import de.redstonecloud.api.redis.broker.packet.defaults.cluster.NodeAuthResponsePacket;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.security.PublicKey;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Log4j2
public class RedstoneNode {
    @Getter
    private static RedstoneNode instance;
    @Getter
    public static String workingDir;
    @Getter
    public static Cache cache;
    @Getter
    public static boolean running = false;

    @Getter private static Broker broker;

    @SneakyThrows
    public static void main(String[] args) {
        workingDir = System.getProperty("user.dir");

        if (!new File("./.node.setup").exists()) Utils.setup();

        System.setProperty("redis.port", NodeConfig.getCfg().get("redis_port").getAsString());
        System.setProperty("redis.bind", NodeConfig.getCfg().get("redis_bind").getAsString());
        System.setProperty("redis.db", NodeConfig.getCfg().get("redis_db").getAsString());

        Thread.sleep(2000);

        cache = new Cache();

        String nodeName = NodeConfig.getCfg().get("node_name").getAsString();

        try {
            log.info(Translator.translate("node.startup.redis"));
            broker = new Broker(nodeName, BrokerHelper.constructRegistry(), "cloud", nodeName);

            broker.listen("cloud", PacketHandler::handle);
            broker.listen(nodeName, PacketHandler::handle);
        } catch (Exception e) {
            log.error(System.getenv("REDIS_IP") != null ? System.getenv("REDIS_IP") : System.getProperty("redis.bind"));
            log.error(System.getenv("REDIS_PORT") != null ? System.getenv("REDIS_PORT") : System.getProperty("redis.port"));
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

    private ConsoleThread consoleThread;
    @Setter
    protected ServerLogger currentLogServer = null;
    protected ServerManager serverManager;
    protected CommandManager commandManager;
    protected Console console;

    protected boolean stopped = false;
    protected boolean authenticated = false;

    protected TaskScheduler scheduler;
    protected KeyCache keyCache;

    public RedstoneNode() {
        instance = this;
        boot();
    }

    public static void createBaseFolders() {
        String[] dirs = {"./servers", "./templates", "./tmp", "./logs", "./template_configs", "./types"};

        for (String dir : dirs) {
            File f = new File(dir);
            if (!f.exists()) {
                f.mkdir();
            }
        }
    }

    public void boot() {
        running = true;

        this.scheduler = new TaskScheduler(new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors()));

        PublicKey publicKey = KeyManager.init();
        this.keyCache = new KeyCache();
        this.keyCache.addKey(NodeConfig.getCfg().get("node_name").getAsString(), publicKey);

        log.info(Translator.translate("node.startup"));

        createBaseFolders();

        this.serverManager = ServerManager.getInstance();
        this.commandManager = new CommandManager();
        commandManager.loadCommands();

        this.console = new Console(this);
        this.consoleThread = new ConsoleThread();
        this.consoleThread.start();

        // Authenticate with master cloud
        authenticate();

        this.scheduler.scheduleRepeatingTask(new CheckTemplateTask(), 3000L);
    }

    private void authenticate() {
        String nodeName = NodeConfig.getCfg().get("node_name").getAsString();
        String authKey = NodeConfig.getCfg().get("auth_key").getAsString();

        AtomicBoolean authSuccess = new AtomicBoolean(false);

        new NodeAuthPacket(nodeName, authKey)
                .setTo("cloud")
                .send(NodeAuthResponsePacket.class, response -> {
                    if (response != null && response.isSuccess()) {
                        log.info(Translator.translate("node.auth.success"));
                        authenticated = true;
                        authSuccess.set(true);
                    } else {
                        log.error(Translator.translate("node.auth.failed", response != null ? response.getMessage() : "No response"));
                        stop();
                    }
                });

        // Wait for authentication
        int attempts = 0;
        while (!authSuccess.get() && attempts < 10) {
            try {
                Thread.sleep(500);
                attempts++;
            } catch (InterruptedException e) {
                log.error("Authentication interrupted", e);
            }
        }

        if (!authSuccess.get()) {
            log.error("Authentication timeout");
            stop();
        }
    }

    public void stop() {
        if (this.stopped || !running) {
            return;
        }

        this.stopped = true;
        running = false;
        this.scheduler.cancelAll();

        try {
            Thread.sleep(200);
            log.info(Translator.translate("node.shutdown.started"));
            boolean a = this.serverManager.stopAll();
            if (a) log.info(Translator.translate("node.shutdown.servers"));
            log.info(Translator.translate("node.shutdown.complete"));
            broker.shutdown();
            this.scheduler.stopScheduler();
        } catch (InterruptedException e) {
            log.error("Error during shutdown: ", e);
        } catch (Exception e) {
            log.error("Unexpected error during shutdown: ", e);
        }

        System.exit(0);
    }

    private class ConsoleThread extends Thread {
        public ConsoleThread() {
            super("Console Thread");
        }

        @Override
        public void run() {
            if (isRunning()) console.start();
        }
    }
}
