package de.redstonecloud.node.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.redstonecloud.node.RedstoneNode;
import de.redstonecloud.node.config.NodeConfig;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Scanner;

@Log4j2
public class Utils {
    public static String[] dropFirstString(String[] input) {
        String[] anstring = new String[input.length - 1];
        System.arraycopy(input, 1, anstring, 0, input.length - 1);
        return anstring;
    }

    public static String readFileFromResources(String filename) throws IOException {
        try (InputStream inputStream = RedstoneNode.class.getClassLoader().getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found! " + filename);
            }
            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
                return scanner.useDelimiter("\\A").next();
            }
        }
    }

    public static void setup() {
        Scanner input = new Scanner(System.in);
        Gson gson = new Gson();

        log.info("RedstoneNode Node Setup");
        log.info("========================");

        String nodeName = setupNodeName(input);
        String authKey = setupAuthKey(input);
        RedisConfig redisConfig = setupRedis(input);

        createBaseStructure();
        copyNodeFiles();
        finalizeSetup(nodeName, authKey, redisConfig);
        showSummary(nodeName, redisConfig);
        waitForUserToStart();
    }

    private static class RedisConfig {
        String bind;
        int port;
        int db;

        RedisConfig(String bind, int port, int db) {
            this.bind = bind;
            this.port = port;
            this.db = db;
        }
    }

    private static String setupNodeName(Scanner input) {
        log.info("Please provide a name for this node (e.g., 'node-1'): ");
        String name = input.nextLine();
        if (name.isEmpty()) {
            name = "node-1";
            log.info("Using default name: {}", name);
        }
        return name;
    }

    private static String setupAuthKey(Scanner input) {
        log.info("Please provide the auth key for this node (must match master cloud configuration): ");
        String authKey = input.nextLine();
        if (authKey.isEmpty()) {
            authKey = generateAuthKey();
            log.warn("No auth key provided. Generated random key: {}", authKey);
            log.warn("Please add this key to the master cloud configuration!");
        }
        return authKey;
    }

    private static String generateAuthKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static RedisConfig setupRedis(Scanner input) {
        String redisBind = "127.0.0.1";
        int intRedisPort = 6379;
        int intRedisDb = 0;

        log.info("Please provide the Redis IP to connect to (default: 127.0.0.1): ");
        String bindInput = input.nextLine();
        if (!bindInput.isEmpty()) {
            redisBind = bindInput;
        }

        log.info("Please provide the Redis port (default: 6379): ");
        try {
            String portInput = input.nextLine();
            if (!portInput.isEmpty()) {
                intRedisPort = Integer.parseInt(portInput);
            }
        } catch (Exception e) {
            log.warn("Provided invalid port, using default port.");
        }

        log.info("Please provide the Redis database (default: 0): ");
        try {
            String dbInput = input.nextLine();
            if (!dbInput.isEmpty()) {
                intRedisDb = Integer.parseInt(dbInput);
            }
        } catch (Exception e) {
            log.warn("Provided invalid database, using default database.");
        }

        return new RedisConfig(redisBind, intRedisPort, intRedisDb);
    }

    private static void createBaseStructure() {
        log.info("Generating basic file structure...");
        RedstoneNode.createBaseFolders();
        log.info("Basic folders generated.");
    }

    private static void copyNodeFiles() {
        log.info("Copying node setup files...");
        try {
            FileUtils.copyURLToFile(Utils.class.getClassLoader().getResource("node.json"), 
                new File("./node.json"));
            FileUtils.copyURLToFile(Utils.class.getClassLoader().getResource("language.json"), 
                new File("./language.json"));
            log.info("Copied node files.");
        } catch (IOException e) {
            log.error("Copying node files failed, shutting down...", e);
            System.exit(0);
        }
    }

    private static void finalizeSetup(String nodeName, String authKey, RedisConfig redisConfig) {
        try {
            JsonObject cfgFile = new JsonObject();
            cfgFile.addProperty("node_name", nodeName);
            cfgFile.addProperty("auth_key", authKey);
            cfgFile.addProperty("redis_port", redisConfig.port);
            cfgFile.addProperty("redis_bind", redisConfig.bind);
            cfgFile.addProperty("redis_db", redisConfig.db);

            Files.writeString(Paths.get(RedstoneNode.workingDir + "/node.json"), cfgFile.toString());
            Files.writeString(Paths.get(RedstoneNode.workingDir + "/.node.setup"),
                    "Node is set up. Do not delete this file or the setup will start again.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void showSummary(String nodeName, RedisConfig redisConfig) {
        log.info("");
        log.info("Node setup completed.");
        log.info("====================");
        log.info("Node name: {}", nodeName);
        log.info("Redis IP: {}", redisConfig.bind);
        log.info("Redis port: {}", redisConfig.port);
        log.info("Redis db: {}", redisConfig.db);
        log.info("====================");
    }

    private static void waitForUserToStart() {
        log.info("");
        log.info("Please press Enter to start the node.");
        try {
            System.in.read(new byte[2]);
        } catch (IOException e) {
            log.error("Error waiting for input", e);
        }
    }
}
