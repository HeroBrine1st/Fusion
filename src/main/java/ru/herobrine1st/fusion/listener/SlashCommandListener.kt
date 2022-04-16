package ru.herobrine1st.fusion.listener

import dev.minn.jda.ktx.CoroutineEventListener
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.utils.TimeUtil
import net.dv8tion.jda.api.utils.TimeUtil.DISCORD_EPOCH
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

        val sent = (event.idLong ushr TimeUtil.TIMESTAMP_OFFSET.toInt()) + DISCORD_EPOCH
        val ping = System.currentTimeMillis() - sent
        assert(ping > 0)
        if(ping > 2000) {
            logger.warn("Got command $ping ms after sending")
        }

        val start = System.currentTimeMillis()
        try {
            command.execute(event)
        } catch (e: Exception) {
            val end = System.currentTimeMillis()
            // TAG USER_INPUT_IN_LOGS (remember log4sh?)
            logger.error("An exception occurred while executing command \"${event.commandString}\"", e)
            logger.error("Execution time: ${end - start} ms")
            if(event.isAcknowledged) event.hook.send(content = "An error occurred while executing this command").await()
            else event.reply("An error occurred while executing this command").setEphemeral(true).await()
        }
    }
}