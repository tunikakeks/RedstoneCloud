package de.redstonecloud.cloud.commands.defaults;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.commands.Command;
import de.redstonecloud.cloud.config.entry.ClusterMode;
import de.redstonecloud.cloud.server.ServerManager;
import de.redstonecloud.cloud.server.Template;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Command to view slave node templates (MASTER only).
 */
public class ClusterCommand extends Command {
    public int argCount = 0;

    public ClusterCommand(String cmd) {
        super(cmd);
    }

    @Override
    protected void onCommand(String[] args) {
        if (!RedstoneCloud.getClusterConfig().enabled()) {
            System.out.println("Clustering is not enabled.");
            return;
        }

        if (RedstoneCloud.getClusterConfig().mode() != ClusterMode.MASTER) {
            System.out.println("This command is only available on MASTER nodes.");
            return;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            listSlaveNodes();
        } else if (args[0].equalsIgnoreCase("templates") && args.length >= 2) {
            showSlaveTemplates(args[1]);
        } else if (args[0].equalsIgnoreCase("all")) {
            showAllTemplates();
        } else {
            System.out.println("Usage:");
            System.out.println("  cluster list - List all slave nodes");
            System.out.println("  cluster templates <node-id> - Show templates for a specific slave");
            System.out.println("  cluster all - Show all templates from all slaves");
        }
    }

    private void listSlaveNodes() {
        ServerManager sm = ServerManager.getInstance();
        Set<String> slaveNodes = sm.getSlaveNodeIds();

        if (slaveNodes.isEmpty()) {
            System.out.println("No slave nodes registered.");
            return;
        }

        System.out.println("\nRegistered Slave Nodes:");
        System.out.println("+-----------------------+------------+");
        System.out.println("| Node ID               | Templates  |");
        System.out.println("+-----------------------+------------+");

        for (String nodeId : slaveNodes) {
            List<String> templates = sm.getSlaveTemplateNames(nodeId);
            System.out.format("| %-21s | %-10d |%n", nodeId, templates.size());
        }
        System.out.println("+-----------------------+------------+");
    }

    private void showSlaveTemplates(String nodeId) {
        ServerManager sm = ServerManager.getInstance();
        List<String> templateNames = sm.getSlaveTemplateNames(nodeId);

        if (templateNames.isEmpty()) {
            System.out.println("No templates found for slave node: " + nodeId);
            return;
        }

        System.out.println("\nTemplates for slave node: " + nodeId);
        System.out.println("+---------------------+---------+------------+------------+");
        System.out.println("| Template            | Type    | Min/Max    | Players    |");
        System.out.println("+---------------------+---------+------------+------------+");

        for (String templateName : templateNames) {
            Template template = sm.getSlaveTemplate(nodeId, templateName);
            if (template != null) {
                System.out.format("| %-19s | %-7s | %-3d/%-6d | %-10d |%n",
                    template.getName(),
                    template.getType().getName(),
                    template.getMinServers(),
                    template.getMaxServers(),
                    template.getMaxPlayers());
            }
        }
        System.out.println("+---------------------+---------+------------+------------+");
    }

    private void showAllTemplates() {
        ServerManager sm = ServerManager.getInstance();
        Map<String, List<Template>> allTemplates = sm.getAllSlaveTemplates();

        if (allTemplates.isEmpty()) {
            System.out.println("No slave templates found.");
            return;
        }

        System.out.println("\nAll Slave Templates:");
        for (Map.Entry<String, List<Template>> entry : allTemplates.entrySet()) {
            String nodeId = entry.getKey();
            List<Template> templates = entry.getValue();

            System.out.println("\n[" + nodeId + "] - " + templates.size() + " templates");
            System.out.println("+---------------------+---------+------------+");
            System.out.println("| Template            | Type    | Min/Max    |");
            System.out.println("+---------------------+---------+------------+");

            for (Template template : templates) {
                System.out.format("| %-19s | %-7s | %-3d/%-6d |%n",
                    template.getName(),
                    template.getType().getName(),
                    template.getMinServers(),
                    template.getMaxServers());
            }
            System.out.println("+---------------------+---------+------------+");
        }
    }
}
