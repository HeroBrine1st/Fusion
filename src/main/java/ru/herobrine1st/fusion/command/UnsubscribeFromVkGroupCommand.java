package ru.herobrine1st.fusion.command;

import org.jetbrains.annotations.NotNull;
import ru.herobrine1st.fusion.api.command.CommandContext;
import ru.herobrine1st.fusion.api.command.CommandExecutor;
import ru.herobrine1st.fusion.api.exception.CommandException;

public class UnsubscribeFromVkGroupCommand implements CommandExecutor {
    @Override
    public void execute(@NotNull CommandContext ctx) throws CommandException {
        ctx.deferReply(true).queue();
    }
}
