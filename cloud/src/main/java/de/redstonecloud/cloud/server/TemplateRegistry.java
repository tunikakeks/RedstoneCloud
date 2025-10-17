package de.redstonecloud.cloud.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.redstonecloud.cloud.RedstoneCloud;
import lombok.extern.log4j.Log4j2;

import java.util.*;

/**
 * Manages template storage and retrieval in Redis for cluster mode.
 * Templates are stored with keys: template:{nodeId}:{templateName}
 */
@Log4j2
public class TemplateRegistry {
    private static final Gson GSON = new Gson();
    private static final String TEMPLATE_KEY_PREFIX = "template:";

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
     * Load a template from Redis for a specific node.
     */
    public static Template loadTemplate(String nodeId, String templateName) {
        String key = buildTemplateKey(nodeId, templateName);
        String data = RedstoneCloud.getCache().get(key);
        
        if (data == null) {
            return null;
        }
        
        return deserializeTemplate(data);
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
     * Get all node IDs that have templates stored in Redis.
     */
    public static Set<String> getAllNodeIds() {
        String pattern = TEMPLATE_KEY_PREFIX + "*";
        Set<String> keys = RedstoneCloud.getCache().keys(pattern);
        
        Set<String> nodeIds = new HashSet<>();
        for (String key : keys) {
            String nodeId = extractNodeId(key);
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
     * Delete all templates for a specific node.
     */
    public static void deleteAllTemplates(String nodeId) {
        List<String> templateNames = getTemplateNames(nodeId);
        for (String templateName : templateNames) {
            deleteTemplate(nodeId, templateName);
        }
        log.debug("Deleted all templates for node {}", nodeId);
    }

    private static String buildTemplateKey(String nodeId, String templateName) {
        return TEMPLATE_KEY_PREFIX + nodeId + ":" + templateName;
    }

    private static String extractTemplateName(String key, String nodeId) {
        String prefix = TEMPLATE_KEY_PREFIX + nodeId + ":";
        if (key.startsWith(prefix)) {
            return key.substring(prefix.length());
        }
        return null;
    }

    private static String extractNodeId(String key) {
        if (!key.startsWith(TEMPLATE_KEY_PREFIX)) {
            return null;
        }
        
        String remainder = key.substring(TEMPLATE_KEY_PREFIX.length());
        int colonIndex = remainder.indexOf(':');
        if (colonIndex > 0) {
            return remainder.substring(0, colonIndex);
        }
        
        return null;
    }

    private static JsonObject serializeTemplate(Template template) {
        JsonObject data = new JsonObject();
        data.addProperty("name", template.getName());
        data.addProperty("type", template.getType().getName());
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

    private static Template deserializeTemplate(String jsonData) {
        JsonObject data = GSON.fromJson(jsonData, JsonObject.class);
        
        String typeName = data.get("type").getAsString();
        ServerType type = ServerManager.getInstance().getTypes().get(typeName);
        
        if (type == null) {
            log.error("Template references unknown server type: {}", typeName);
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
}
