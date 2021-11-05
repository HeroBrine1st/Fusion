package ru.herobrine1st.fusion.permission;

import org.jetbrains.annotations.NotNull;
import ru.herobrine1st.fusion.Config;
import ru.herobrine1st.fusion.api.command.CommandContext;
import ru.herobrine1st.fusion.api.command.PermissionHandler;

public class OwnerPermissionHandler extends PermissionHandler {
    @Override
    public boolean shouldBeExecuted(CommandContext ctx) {
        return ctx.getUser().getId().equals(Config.getOwnerId());
    }

    @Override
    public @NotNull String requirements(CommandContext ctx) {
        return "You should be owner of this bot";
    }
}
