package ru.herobrine1st.fusion.module.base.command;

import org.jetbrains.annotations.NotNull;
import ru.herobrine1st.fusion.api.command.CommandContext;
import ru.herobrine1st.fusion.api.command.CommandExecutor;
import ru.herobrine1st.fusion.api.command.FusionOptionData;
import ru.herobrine1st.fusion.api.command.build.FusionBaseCommand;
import ru.herobrine1st.fusion.api.command.build.FusionCommand;
import ru.herobrine1st.fusion.api.command.build.FusionSubcommandGroup;
import ru.herobrine1st.fusion.internal.Config;
import ru.herobrine1st.fusion.internal.manager.CommandManagerImpl;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static ru.herobrine1st.fusion.internal.command.SlashCommandBuilder.hasSlashSupport;
import static ru.herobrine1st.fusion.internal.manager.CommandManagerImpl.usage;

public class HelpCommand implements CommandExecutor {
    @Override
    public void execute(@NotNull CommandContext ctx) {
        var optionalCommand = ctx.<String>getOne("command");
        var prefix = Config.INSTANCE.getDiscordPrefix();
        Stream<FusionCommand<?>> commandDataStream = CommandManagerImpl.INSTANCE.commands.stream()
                .filter(it -> !hasSlashSupport(it));
        if (optionalCommand.isEmpty()) {
            List<FusionCommand<?>> commandDataList = commandDataStream
                    .sorted(Comparator.comparing(FusionOptionData::getName))
                    .toList();
            if (commandDataList.isEmpty()) {
                ctx.reply(ctx.getEmbedBase()
                        .setDescription("Нет классических команд!")
                        .setColor(ctx.getErrorColor())
                        .build()).queue();
                return;
            }
            var embed = ctx.getEmbedBase()
                    .setDescription("Отображены только классические команды.\n");
            if (commandDataList.size() > 25) {
                embed.appendDescription("Отображено только 25 команд из %s".formatted(commandDataList.size()));
            }
            commandDataList.stream().limit(25).forEach(data -> embed.addField(prefix + data.getName() + " " + usage(data), data.getDescription(), false));
            ctx.reply(embed.build()).queue();
        } else {
            var split = optionalCommand.get().split(" ");
            Optional<? extends FusionBaseCommand<?, ?>> commandDataOptional;
            if(split.length == 1) {
                commandDataOptional = commandDataStream.filter(it -> it.getName().equals(split[0])).findAny();
            } else if(split.length == 3){
                commandDataOptional = commandDataStream
                        .filter(it -> it.getName().equals(split[0]))
                        .filter(FusionBaseCommand::hasSubcommandGroups)
                        .limit(1)
                        .flatMap(it -> it.getOptions().stream())
                        .map(FusionSubcommandGroup.class::cast)
                        .filter(it -> it.getName().equals(split[1]))
                        .limit(1)
                        .flatMap(it -> it.getSubcommandData().stream())
                        .filter(it -> it.getName().equals(split[2]))
                        .findAny();
            } else if(split.length == 2) {
                var optionalSubcommandGroupData = commandDataStream
                        .filter(it -> it.getName().equals(split[0]))
                        .filter(FusionBaseCommand::hasSubcommandGroups)
                        .limit(1)
                        .flatMap(it -> it.getOptions().stream())
                        .map(FusionSubcommandGroup.class::cast)
                        .filter(it -> it.getName().equals(split[1]))
                        .findAny();
                if(optionalSubcommandGroupData.isEmpty()) {
                    ctx.reply(ctx.getEmbedBase()
                            .setDescription("Команда не найдена!")
                            .setColor(ctx.getErrorColor())
                            .build()
                    ).queue();
                    return;
                }
                var subcommandGroupData = optionalSubcommandGroupData.get();
                var embed = ctx.getEmbedBase().setDescription(subcommandGroupData.getDescription());
                embed.addField("Использование", prefix + String.join(" ", split) + " " + usage(subcommandGroupData), false);
                subcommandGroupData.getSubcommandData().forEach(it -> embed.addField(it.getName(), it.getDescription(), false));
                ctx.reply(embed.build()).queue();
                return;
            } else {
                commandDataOptional = Optional.empty();
            }
            if (commandDataOptional.isEmpty()) {
                ctx.reply(ctx.getEmbedBase()
                        .setDescription("Команда не найдена!")
                        .setColor(ctx.getErrorColor())
                        .build()
                ).queue();
                return;
            }
            var commandData = commandDataOptional.get();
            var embed = ctx.getEmbedBase()
                    .setTitle(commandData.getShortName())
                    .setDescription(commandData.getDescription())
                    .setAuthor(ctx.getCommand().getShortName());
            embed.addField("Использование", prefix + String.join(" ", split) + " " + usage(commandData), false);
            commandData.getOptions().forEach(it -> embed.addField(it.getName(), it.getDescription(), false));
            ctx.reply(embed.build()).queue();
        }
    }
}
