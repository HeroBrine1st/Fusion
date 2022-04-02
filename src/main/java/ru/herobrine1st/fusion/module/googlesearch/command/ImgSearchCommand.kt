package ru.herobrine1st.fusion.module.googlesearch.command

import com.fasterxml.jackson.databind.JsonNode
import dev.minn.jda.ktx.EmbedBuilder
import dev.minn.jda.ktx.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import ru.herobrine1st.fusion.Config
import java.awt.Color

object ImgSearchCommand : AbstractSearchCommand() {
    private val URL: HttpUrl = "https://www.googleapis.com/customsearch/v1".toHttpUrl()

    override fun getUrl(event: SlashCommandInteractionEvent): HttpUrl {
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

    override fun getMessage(event: SlashCommandInteractionEvent, items: JsonNode, index: Int): Message {
        assert(items.isArray)
        val image = items[index]
        return MessageBuilder(
            embed = EmbedBuilder(
                title = image["title"].asText(),
                url = image["image"]["contextLink"].asText(),
                image = image["link"].asText(),
                footerText = "Image ${index + 1}/${items.size()}",
            ) {
                if (image["mime"].asText() == "image/svg+xml")
                    description = "SVG images may not display on some clients."
                builder.setColor(Color.GREEN)
            }.build()
        ).build()
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