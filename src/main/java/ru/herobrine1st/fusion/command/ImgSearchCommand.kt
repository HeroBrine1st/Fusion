package ru.herobrine1st.fusion.command

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import dev.minn.jda.ktx.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.Config
import ru.herobrine1st.fusion.listener.awaitForInteraction
import ru.herobrine1st.fusion.util.objectMapper
import java.io.IOException

object ImgSearchCommand : ICommand {
    private val URL: HttpUrl = "https://www.googleapis.com/customsearch/v1".toHttpUrl()
    private val logger = LoggerFactory.getLogger(ImgSearchCommand::class.java)

    private val okHttpClient = OkHttpClient()

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
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
        }
        val items = objectNode["items"]
        if (items.isEmpty) event.reply("No results")
        index = index.coerceIn(0 until items.size())
        var message = updateMessage(event.hook, items, index)
        while(true) {
            val buttonInteractionEvent = message.awaitForInteraction()
            buttonInteractionEvent.deferEdit().await()
            assert(buttonInteractionEvent is ButtonInteractionEvent) {
                "Expected ButtonInteractionEvent, got ${buttonInteractionEvent::class.simpleName}"
            }
            when(buttonInteractionEvent.componentId) {
                "first" -> index = 0
                "prev" -> index -= 1
                "next" -> index += 1
                "last" -> index = items.size() - 1
            }
            assert(index in 0 until items.size())
            message = updateMessage(buttonInteractionEvent.hook, items, index)
        }
    }

    private suspend fun updateMessage(hook: InteractionHook, items: JsonNode, index: Int): Message {
        val image = items[index]
        return hook.editOriginal(
            MessageBuilder().setEmbeds(
                EmbedBuilder().apply {
                    setTitle(image["title"].asText(), image["image"]["contextLink"].asText())
                    setImage(image["link"].asText())
                    if (image["mime"].asText() == "image/svg+xml")
                        setDescription("SVG images may not display on some clients.")
                    setFooter("Image ${index + 1}/${items.size()}")
                }.build()
            ).setActionRows(
                ActionRow.of(
                    Button.secondary("first", "<< First").withDisabled(index == 0),
                    Button.primary("prev", "< Prev").withDisabled(index == 0),
                    Button.primary("next", "Next >").withDisabled(index == items.size() - 1),
                    Button.secondary("last", "Last >>").withDisabled(index == items.size() - 1)
                ),
                ActionRow.of(
                    Button.danger("delete", "Delete this message")
                )
            ).build()
        ).await()
    }

    private fun getUrl(event: SlashCommandInteractionEvent): HttpUrl {
        val query = event.getOption("query", OptionMapping::getAsString)
        val type: String? = event.getOption("type", OptionMapping::getAsString)

        val safe = event.getOption("safe", "active", OptionMapping::getAsString)

        val nsfwAllowed: Boolean = with(event.channel) {
            if (this is TextChannel) isNSFW else true
        }

        return URL.newBuilder().apply {
            addQueryParameter("num", "10")
            addQueryParameter("start", "1")
            addQueryParameter("searchType", "image")
            addQueryParameter("cx", Config.googleCustomSearchEngineId)
            addQueryParameter("key", Config.googleCustomSearchApiKey)
            addQueryParameter("safe", if (nsfwAllowed) safe else "active")
            addQueryParameter("q", query)
            type?.let { addQueryParameter("fileType", type) }
        }.build()
    }

    override val commandData by lazy {
        Commands.slash("img", "Search images")
            .addOption(OptionType.STRING, "query", "Search query", true)
            .addOption(OptionType.STRING, "type", "File type")
            .addOptions(
                OptionData(
                    OptionType.INTEGER, "index", "Image index",
                    false, false
                )
                    .setRequiredRange(0, 9),
                OptionData(
                    OptionType.STRING, "safe", "Whether to enable safe search",
                    false, false
                )
                    .addChoice("No", "off")
                    .addChoice("Yes", "active")
            )
    }
}
