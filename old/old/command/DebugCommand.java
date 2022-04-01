package ru.herobrine1st.fusion.old.command;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.herobrine1st.fusion.api.command.CommandContext;
import ru.herobrine1st.fusion.api.command.CommandExecutor;
import ru.herobrine1st.fusion.api.exception.CommandException;

public class DebugCommand implements CommandExecutor {
    private static final Logger logger = LoggerFactory.getLogger("DebugCommmand");
    @Override
    public void execute(@NotNull CommandContext ctx) throws CommandException {
        logger.debug("Before deferReply: %d".formatted(System.currentTimeMillis()));
        ctx.deferReply().queue((it) -> logger.debug("Right after deferReply: %d".formatted(System.currentTimeMillis())));
        logger.debug("Before sendMessage: %d".formatted(System.currentTimeMillis()));
        ctx.getHook().sendMessage("Success").queue((it) -> logger.debug("Right after sendMessage: %d".formatted(System.currentTimeMillis())));
        logger.debug("End: %d".formatted(System.currentTimeMillis()));
    }
}
