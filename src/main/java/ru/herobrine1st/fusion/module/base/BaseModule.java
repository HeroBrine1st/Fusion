package ru.herobrine1st.fusion.module.base;

import net.dv8tion.jda.api.hooks.SubscribeEvent;
import ru.herobrine1st.fusion.api.Fusion;
import ru.herobrine1st.fusion.api.annotation.FusionModule;
import ru.herobrine1st.fusion.api.command.args.GenericArguments;
import ru.herobrine1st.fusion.api.command.build.FusionCommand;
import ru.herobrine1st.fusion.api.event.FusionInitializationEvent;
import ru.herobrine1st.fusion.module.base.command.HelpCommand;

@FusionModule(id = "base")
public class BaseModule {
    @SubscribeEvent
    public void onInit(FusionInitializationEvent event) {
        Fusion.getCommandManager().registerCommand(FusionCommand.withArguments("help", "Помощь по классическим командам")
                .addOptions(GenericArguments.string("command", "Команда, для которой необходима страница помощи", true).setRequired(false))
                .setExecutor(new HelpCommand())
                .setShortName("Помощь")
        );
    }
}
