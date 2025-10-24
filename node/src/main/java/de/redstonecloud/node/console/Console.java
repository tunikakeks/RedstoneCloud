package de.redstonecloud.node.console;

import de.redstonecloud.node.RedstoneNode;
import de.redstonecloud.node.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

@Log4j2
@RequiredArgsConstructor
public class Console extends SimpleTerminalConsole {
    private final RedstoneNode server;

    @Override
    protected boolean isRunning() {
        return RedstoneNode.isRunning();
    }

    @Override
    protected void runCommand(String command) {
        boolean hasLogServer = server.getCurrentLogServer() != null;
        if (!hasLogServer) {
            String cmd = command.split(" ")[0];
            String[] args = Utils.dropFirstString(command.split(" "));
            server.getCommandManager().executeCommand(cmd, args);
        } else {
            if (command.equalsIgnoreCase("_exit")) {
                server.getCurrentLogServer().disableConsoleLogging();
                server.setCurrentLogServer(null);
                log.info("Exited console");
            } else {
                server.getCurrentLogServer().getServer().writeConsole(command);
            }
        }
    }

    @Override
    protected void shutdown() {
        server.stop();
    }

    @Override
    protected LineReader buildReader(LineReaderBuilder builder) {
        builder.completer(new ConsoleCompleter(server));
        builder.appName("RedstoneNode");
        builder.option(LineReader.Option.HISTORY_BEEP, false);
        builder.option(LineReader.Option.HISTORY_IGNORE_DUPS, true);
        builder.option(LineReader.Option.HISTORY_IGNORE_SPACE, true);
        return super.buildReader(builder);
    }
}