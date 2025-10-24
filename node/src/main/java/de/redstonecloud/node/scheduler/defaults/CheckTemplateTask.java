package de.redstonecloud.node.scheduler.defaults;

import de.redstonecloud.node.server.ServerManager;
import de.redstonecloud.node.server.Template;
import de.redstonecloud.node.scheduler.task.Task;

public class CheckTemplateTask extends Task {
    @Override
    protected void onRun(long currentMillis) {
        for(Template template : ServerManager.getInstance().getTemplates().values())
            template.checkServers();
    }
}
