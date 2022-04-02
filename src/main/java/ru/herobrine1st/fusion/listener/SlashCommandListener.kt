package ru.herobrine1st.fusion.listener

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.command.ICommand
import ru.herobrine1st.fusion.module.googlesearch.command.ImgSearchCommand
import ru.herobrine1st.fusion.module.googlesearch.command.YoutubeSearchCommand

object SlashCommandListener : EventListener {
    private val logger = LoggerFactory.getLogger(SlashCommandListener::class.java)
    override fun onEvent(event: GenericEvent) {
        if (event !is SlashCommandInteractionEvent) return

        val command: ICommand = when (event.name) {
            ImgSearchCommand.commandData.name -> ImgSearchCommand
            YoutubeSearchCommand.commandData.name -> YoutubeSearchCommand
            else -> {
                logger.warn("Invalid command name (${event.name}) received. Forgot to update commands?")
                return
            }
        }

        CoroutineScope(Dispatchers.Default).launch {
            try {
                command.execute(event)
            } catch (t: Throwable) {
                logger.error("An exception occurred while executing command ${event.commandPath}", t)
            }
        }
    }
}