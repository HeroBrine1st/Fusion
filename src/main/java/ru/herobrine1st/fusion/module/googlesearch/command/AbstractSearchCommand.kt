package ru.herobrine1st.fusion.module.googlesearch.command

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import dev.minn.jda.ktx.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.command.ICommand
import ru.herobrine1st.fusion.listener.awaitInteraction
import ru.herobrine1st.fusion.util.objectMapper
import java.io.IOException

abstract class AbstractSearchCommand : ICommand {

    private val logger = LoggerFactory.getLogger(AbstractSearchCommand::class.java)

    private val okHttpClient = OkHttpClient()

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply().await()
        var index = event.getOption("index", 0) { it.asInt }
        val (response, objectNode) = try {
            withContext(Dispatchers.IO) {
                okHttpClient.newCall(
                    Request.Builder()
                        .url(getUrl(event))
                        .addHeader("Accept", "application/json")
                        .build()
                ).execute().use {
                    val objectNode = objectMapper.readValue<ObjectNode>(it.body!!.charStream())
                    return@withContext it to objectNode
                }
            }
        } catch (e: IOException) {
            logger.error("An error occurred while executing request", e)
            event.hook.sendMessage("An error occurred while executing request").await()
            return
        }
        if (!response.isSuccessful) {
            val error = objectNode["error"]
            if (error != null && !error.isNull) {
                val status = error["status"].asText()
                if (status == "RESOURCE_EXHAUSTED") {
                    event.hook.sendMessage("Reached API daily limit. Try this command later.").await()
                } else {
                    event.hook.sendMessage("${error["message"].asText()} (status $status)").await()
                }
            } else {
                event.hook.sendMessage("Unknown HTTP error occurred. Code: ${response.code}").await()
            }
            return
        }
        val items = objectNode["items"]
        if (items.isEmpty) event.reply("No results")
        index = index.coerceIn(0 until items.size())
        var message = updateMessage(event, items, index)
        while (true) {
            val buttonInteractionEvent = message.awaitInteraction()
            if (buttonInteractionEvent.user.idLong != event.user.idLong) continue
            buttonInteractionEvent.deferEdit().await()
            if (buttonInteractionEvent !is ButtonInteractionEvent)
                throw RuntimeException("Expected ButtonInteractionEvent, got ${buttonInteractionEvent::class.simpleName}")

            when (buttonInteractionEvent.componentId) {
                "first" -> index = 0
                "prev" -> index -= 1
                "next" -> index += 1
                "last" -> index = items.size() - 1
            }
            assert(index in 0 until items.size())
            message = updateMessage(event, items, index)
        }
    }

    private suspend fun updateMessage(event: SlashCommandInteractionEvent, items: JsonNode, index: Int): Message {
        return event.hook.editOriginal(getMessage(event, items, index))
            .setActionRows(
                ActionRow.of(
                    Button.secondary("first", "<< First").withDisabled(index == 0),
                    Button.primary("prev", "< Prev").withDisabled(index == 0),
                    Button.primary("next", "Next >").withDisabled(index == items.size() - 1),
                    Button.secondary("last", "Last >>").withDisabled(index == items.size() - 1)
                ),
                ActionRow.of(
                    Button.danger("delete", "Delete this message")
                )
            ).await()
    }

    protected abstract fun getUrl(event: SlashCommandInteractionEvent): HttpUrl
    protected abstract fun getMessage(event: SlashCommandInteractionEvent, items: JsonNode, index: Int): Message
}
