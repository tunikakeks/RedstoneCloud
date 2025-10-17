package de.redstonecloud.cloud.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.redstonecloud.cloud.RedstoneCloud;
import lombok.extern.log4j.Log4j2;

import java.util.*;

/**
 * Manages template and server type storage and retrieval in Redis for cluster mode.
 * Templates are stored with keys: template:{nodeId}:{templateName}
 * Server types are stored with keys: type:{nodeId}:{typeName}
 */
@Log4j2
public class TemplateRegistry {
    private static final Gson GSON = new Gson();
    private static final String TEMPLATE_KEY_PREFIX = "template:";
    private static final String TYPE_KEY_PREFIX = "type:";

    /**
     * Store a template in Redis for a specific node.
     */
    public static void storeTemplate(String nodeId, Template template) {
        String key = buildTemplateKey(nodeId, template.getName());
        JsonObject data = serializeTemplate(template);
        
        RedstoneCloud.getCache().set(key, data.toString());
        log.debug("Stored template {} for node {}", template.getName(), nodeId);
    }

    /**
     * Store a server type in Redis for a specific node.
     */
    public static void storeServerType(String nodeId, ServerType type) {
        String key = buildTypeKey(nodeId, type.name());
        JsonObject data = serializeServerType(type);
        
        RedstoneCloud.getCache().set(key, data.toString());
        log.debug("Stored server type {} for node {}", type.name(), nodeId);
    }

    /**
     * Load a template from Redis for a specific node.
     */
    public static Template loadTemplate(String nodeId, String templateName) {
        String key = buildTemplateKey(nodeId, templateName);
        String data = RedstoneCloud.getCache().get(key);
        
        if (data == null) {
            return null;
        }
        
        return deserializeTemplate(data, nodeId);
    }

    /**
     * Load a server type from Redis for a specific node.
     */
    public static ServerType loadServerType(String nodeId, String typeName) {
        String key = buildTypeKey(nodeId, typeName);
        String data = RedstoneCloud.getCache().get(key);
        
        if (data == null) {
            return null;
        }
        
        return deserializeServerType(data);
    }

    /**
     * Get all template names for a specific node.
     */
    public static List<String> getTemplateNames(String nodeId) {
        String pattern = TEMPLATE_KEY_PREFIX + nodeId + ":*";
        Set<String> keys = RedstoneCloud.getCache().keys(pattern);
        
        List<String> templateNames = new ArrayList<>();
        for (String key : keys) {
            String templateName = extractTemplateName(key, nodeId);
            if (templateName != null) {
                templateNames.add(templateName);
            }
        }
        
        return templateNames;
    }

    /**
     * Get all server type names for a specific node.
     */
    public static List<String> getServerTypeNames(String nodeId) {
        String pattern = TYPE_KEY_PREFIX + nodeId + ":*";
        Set<String> keys = RedstoneCloud.getCache().keys(pattern);
        
        List<String> typeNames = new ArrayList<>();
        for (String key : keys) {
            String typeName = extractTypeName(key, nodeId);
            if (typeName != null) {
                typeNames.add(typeName);
            }
        }
        
        return typeNames;
    }

    /**
     * Get all node IDs that have templates stored in Redis.
     */
    public static Set<String> getAllNodeIds() {
        String pattern = TEMPLATE_KEY_PREFIX + "*";
        Set<String> keys = RedstoneCloud.getCache().keys(pattern);
        
        Set<String> nodeIds = new HashSet<>();
        for (String key : keys) {
            String nodeId = extractNodeId(key, TEMPLATE_KEY_PREFIX);
            if (nodeId != null) {
                nodeIds.add(nodeId);
            }
        }
        
        return nodeIds;
    }

    /**
     * Delete a template from Redis.
     */
    public static void deleteTemplate(String nodeId, String templateName) {
        String key = buildTemplateKey(nodeId, templateName);
        RedstoneCloud.getCache().delete(key);
        log.debug("Deleted template {} for node {}", templateName, nodeId);
    }

    /**
     * Delete a server type from Redis.
     */
    public static void deleteServerType(String nodeId, String typeName) {
        String key = buildTypeKey(nodeId, typeName);
        RedstoneCloud.getCache().delete(key);
        log.debug("Deleted server type {} for node {}", typeName, nodeId);
    }

    /**
     * Delete all templates for a specific node.
     */
    public static void deleteAllTemplates(String nodeId) {
        List<String> templateNames = getTemplateNames(nodeId);
        for (String templateName : templateNames) {
            deleteTemplate(nodeId, templateName);
        }
        log.debug("Deleted all templates for node {}", nodeId);
    }

    /**
     * Delete all server types for a specific node.
     */
    public static void deleteAllServerTypes(String nodeId) {
        List<String> typeNames = getServerTypeNames(nodeId);
        for (String typeName : typeNames) {
            deleteServerType(nodeId, typeName);
        }
        log.debug("Deleted all server types for node {}", nodeId);
    }

    private static String buildTemplateKey(String nodeId, String templateName) {
        return TEMPLATE_KEY_PREFIX + nodeId + ":" + templateName;
    }

    private static String buildTypeKey(String nodeId, String typeName) {
        return TYPE_KEY_PREFIX + nodeId + ":" + typeName;
    }

    private static String extractTemplateName(String key, String nodeId) {
        String prefix = TEMPLATE_KEY_PREFIX + nodeId + ":";
        if (key.startsWith(prefix)) {
            return key.substring(prefix.length());
        }
        return null;
    }

    private static String extractTypeName(String key, String nodeId) {
        String prefix = TYPE_KEY_PREFIX + nodeId + ":";
        if (key.startsWith(prefix)) {
            return key.substring(prefix.length());
        }
        return null;
    }

    private static String extractNodeId(String key, String prefix) {
        if (!key.startsWith(prefix)) {
            return null;
        }
        
        String remainder = key.substring(prefix.length());
        int colonIndex = remainder.indexOf(':');
        if (colonIndex > 0) {
            return remainder.substring(0, colonIndex);
        }
        
        return null;
    }

    private static JsonObject serializeTemplate(Template template) {
        JsonObject data = new JsonObject();
        data.addProperty("name", template.getName());
        data.addProperty("type", template.getType().name());
        data.addProperty("maxPlayers", template.getMaxPlayers());
        data.addProperty("minServers", template.getMinServers());
        data.addProperty("maxServers", template.getMaxServers());
        data.addProperty("staticServer", template.isStaticServer());
        data.addProperty("stopOnEmpty", template.isStopOnEmpty());
        data.addProperty("shutdownTimeMs", template.getShutdownTimeMs());
        data.addProperty("seperator", template.getSeperator());
        data.addProperty("maxBootTimeMs", template.getMaxBootTimeMs());
        return data;
    }

    private static JsonObject serializeServerType(ServerType type) {
        JsonObject data = new JsonObject();
        data.addProperty("name", type.name());
        
        JsonArray startCmd = new JsonArray();
        for (String cmd : type.startCommand()) {
            startCmd.add(cmd);
        }
        data.add("startCommand", startCmd);
        
        data.addProperty("isProxy", type.isProxy());
        data.addProperty("logsPath", type.logsPath());
        data.addProperty("portSettingFile", type.portSettingFile());
        data.addProperty("portSettingPlaceholder", type.portSettingPlaceholder());
        data.addProperty("stopCommand", type.stopCommand());
        
        return data;
    }

    private static Template deserializeTemplate(String jsonData, String nodeId) {
        JsonObject data = GSON.fromJson(jsonData, JsonObject.class);
        
        String typeName = data.get("type").getAsString();
        
        // Try to load type from Redis for this node first
        ServerType type = loadServerType(nodeId, typeName);
        
        // Fall back to local types if not found
        if (type == null) {
            type = ServerManager.getInstance().getTypes().get(typeName);
        }
        
        if (type == null) {
            log.error("Template references unknown server type: {} for node {}", typeName, nodeId);
            return null;
        }
        
        return Template.builder()
                .name(data.get("name").getAsString())
                .type(type)
                .maxPlayers(data.get("maxPlayers").getAsInt())
                .minServers(data.get("minServers").getAsInt())
                .maxServers(data.get("maxServers").getAsInt())
                .staticServer(data.get("staticServer").getAsBoolean())
                .stopOnEmpty(data.has("stopOnEmpty") && data.get("stopOnEmpty").getAsBoolean())
                .shutdownTimeMs(data.has("shutdownTimeMs") ? data.get("shutdownTimeMs").getAsInt() : 5000)
                .seperator(data.has("seperator") ? data.get("seperator").getAsString() : "-")
                .maxBootTimeMs(data.has("maxBootTimeMs") ? data.get("maxBootTimeMs").getAsLong() : 60000)
                .build();
    }

    private static ServerType deserializeServerType(String jsonData) {
        JsonObject data = GSON.fromJson(jsonData, JsonObject.class);
        
        String name = data.get("name").getAsString();
        
        JsonArray startCmdArray = data.getAsJsonArray("startCommand");
        String[] startCommand = new String[startCmdArray.size()];
        for (int i = 0; i < startCmdArray.size(); i++) {
            startCommand[i] = startCmdArray.get(i).getAsString();
        }
        
        boolean isProxy = data.get("isProxy").getAsBoolean();
        String logsPath = data.has("logsPath") && !data.get("logsPath").isJsonNull() 
            ? data.get("logsPath").getAsString() : null;
        String portSettingFile = data.get("portSettingFile").getAsString();
        String portSettingPlaceholder = data.get("portSettingPlaceholder").getAsString();
        String stopCommand = data.get("stopCommand").getAsString();
        
        return new ServerType(name, startCommand, isProxy, logsPath, 
                             portSettingFile, portSettingPlaceholder, stopCommand);
    }
}
