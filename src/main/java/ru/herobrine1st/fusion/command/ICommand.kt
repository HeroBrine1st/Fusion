package ru.herobrine1st.fusion.command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

interface ICommand {
    suspend fun execute(event: SlashCommandInteractionEvent)
    val commandData: SlashCommandData
}