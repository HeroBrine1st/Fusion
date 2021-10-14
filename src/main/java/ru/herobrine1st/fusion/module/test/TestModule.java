package ru.herobrine1st.fusion.module.test;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import ru.herobrine1st.fusion.api.Fusion;
import ru.herobrine1st.fusion.api.annotation.FusionModule;
import ru.herobrine1st.fusion.api.command.PermissionHandler;
import ru.herobrine1st.fusion.api.command.args.GenericArguments;
import ru.herobrine1st.fusion.api.command.build.FusionCommand;
import ru.herobrine1st.fusion.api.command.build.FusionSubcommand;
import ru.herobrine1st.fusion.api.command.build.FusionSubcommandGroup;
import ru.herobrine1st.fusion.api.event.FusionInitializationEvent;

import java.util.concurrent.TimeUnit;

@FusionModule(id = "testmodule")
public class TestModule {
    @SubscribeEvent
    public void onInit(FusionInitializationEvent event) {
        Fusion.getCommandManager().registerCommand(FusionCommand.withArguments("fusiontest", "Тестовая команда")
                .setTesting(true)
                .addOptions(
                        GenericArguments.string("string", "Пиши блять сюда текст", true)
                                .addChoice("test", "Тест")
                                .addChoice("test2", "Тест2"),
                        GenericArguments.integer("integer", "Пиши блять сюда число")
                                .addChoice("test", 1L)
                                .addChoice("test2", 2L)
                )
                .setExecutor(ctx -> ctx.reply(ctx.getEmbedBase()
                                        .setDescription(ctx.<String>getOne("string").orElse("Успешный тест!"))
                                        .addField("Тест 2", ctx.<Long>getOne("integer").orElse(0L).toString(), false)
                                        .build(),
                                ActionRow.of(Button.danger("reply_10_seconds", "Ответить через 10 секунд")))
                        .flatMap(it -> ctx.getButtonClickEventRestAction())
                        .delay(10, TimeUnit.SECONDS)
                        .flatMap(buttonClickEvent -> ctx.reply(ctx.getEmbedBase()
                                .setDescription(buttonClickEvent.getComponentId())
                                .build())
                        )
                        .queue(null, ctx::replyException)
                ));
        Fusion.getCommandManager().registerCommand(FusionCommand.withSubcommandGroups("test2", "Тестовая команда с группами")
                .setTesting(true)
                .setPermissionHandler(new PermissionHandler.Typed(PermissionHandler.CommandType.MESSAGE))
                .addOptions(new FusionSubcommandGroup("group", "Группа")
                        .addSubcommands(
                                new FusionSubcommand("subcommand", "Субкоманда")
                                        .setExecutor(ctx -> ctx.reply(ctx.getEmbedBase().setDescription("123").build()).queue())
                        )
                )
        );
        Fusion.getCommandManager().registerCommand(FusionCommand.withSubcommands("test3", "Тестовая команда с субкомандами")
                .setTesting(true)
                .setPermissionHandler(new PermissionHandler.Typed(PermissionHandler.CommandType.MESSAGE))
                .addOptions(
                        new FusionSubcommand("subcommand", "Субкоманда")
                                .setExecutor(ctx -> ctx.reply(ctx.getEmbedBase().setDescription("123").build()).queue())
                )
        );

    }

    @SubscribeEvent
    public void onMessage(MessageReceivedEvent event) {
        if (event.getMessage().getEmbeds().isEmpty()) return;
        var title = event.getMessage().getEmbeds().get(0).getTitle();
        if (title == null) return;
        if (title.contains("удалил своё сообщение!") || title.contains("изменил своё сообщение!")) {
            event.getMessage().delete().queue();
        }
    }
}
