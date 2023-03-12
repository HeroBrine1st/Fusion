package ru.herobrine1st.fusion.module.vk.command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import ru.herobrine1st.fusion.command.ICommand

private const val SUBSCRIBE = "subscribe"
private const val UNSUBSCRIBE = "unsubscribe"
private const val FETCH = "fetch"

object VkCommand : ICommand {
    override suspend fun execute(event: SlashCommandInteractionEvent) {
        when (event.subcommandName) {
            SUBSCRIBE -> SubscribeSubcommand.execute(event)
            UNSUBSCRIBE -> UnsubscribeSubcommand.execute(event)
            FETCH -> FetchCommand.execute(event)
            else -> throw IllegalArgumentException()
        }
    }

    override val commandData: SlashCommandData by lazy {
        Commands.slash("vk", "VK social network related commands")
            .addSubcommands(
                SubcommandData(SUBSCRIBE, "Subscribe to VK group")
                    .addOption(OptionType.STRING, SubscribeSubcommand.URL_ARGUMENT, "Link to group", true),
                SubcommandData(UNSUBSCRIBE, "Unsubscribe from VK group"),
                SubcommandData(FETCH, "Fetch post from vk and show here")
                    .addOption(OptionType.STRING, FetchCommand.URL_ARGUMENT, "Link to post", true)
                    .addOption(OptionType.BOOLEAN, FetchCommand.EPHEMERAL_ARGUMENT, "Set to false if you want the result to be seen by everyone", false)
            )
    }
}