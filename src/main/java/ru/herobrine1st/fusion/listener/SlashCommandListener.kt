package ru.herobrine1st.fusion.listener

import dev.minn.jda.ktx.CoroutineEventListener
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.command.ICommand
import ru.herobrine1st.fusion.module.googlesearch.command.ImgSearchCommand
import ru.herobrine1st.fusion.module.googlesearch.command.YoutubeSearchCommand
import ru.herobrine1st.fusion.module.vk.command.VkCommand

object SlashCommandListener : CoroutineEventListener {
    private val logger = LoggerFactory.getLogger(SlashCommandListener::class.java)
    override suspend fun onEvent(event: GenericEvent) {
        if (event !is SlashCommandInteractionEvent) return

        val command: ICommand = when (event.name) {
            ImgSearchCommand.commandData.name -> ImgSearchCommand
            YoutubeSearchCommand.commandData.name -> YoutubeSearchCommand
            VkCommand.commandData.name -> VkCommand
            else -> {
                logger.warn("Invalid command name (${event.name}) received. Forgot to update commands?")
                return
            }
        }

        try {
            command.execute(event)
        } catch (e: Exception) {
            // TAG USER_INPUT_IN_LOGS (remember log4sh?)
            logger.error("An exception occurred while executing command \"${event.commandString}\"", e)
        }
    }
}