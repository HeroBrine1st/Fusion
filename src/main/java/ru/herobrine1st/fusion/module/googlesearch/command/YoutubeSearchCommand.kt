package ru.herobrine1st.fusion.module.googlesearch.command

import com.fasterxml.jackson.databind.JsonNode
import dev.minn.jda.ktx.messages.MessageEdit
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import ru.herobrine1st.fusion.Config.youtubeSearchApiKey
import ru.herobrine1st.fusion.util.addChoices

object YoutubeSearchCommand : AbstractSearchCommand() {
    private val URL = "https://www.googleapis.com/youtube/v3/search".toHttpUrl()

    override fun getUrl(event: SlashCommandInteractionEvent): HttpUrl {
        return URL.newBuilder()
            .addQueryParameter("part", "snippet")
            .addQueryParameter(
                "type",
                event.getOption("type", "video") { it.asString }
            )
            .addQueryParameter("key", youtubeSearchApiKey)
            .addQueryParameter("maxResults", event.getOption("max", 50) { it.asLong }.toString())
            .addQueryParameter("q", event.getOption("query") { it.asString })
            .build()
    }

    private fun getUrl(json: JsonNode): String {
        val idObject = json["id"]
        return when (idObject["kind"].asText()) {
            "youtube#video" -> "https://youtube.com/watch?v=" + idObject["videoId"].asText()
            "youtube#channel" -> "https://youtube.com/channel/" + idObject["channelId"].asText()
            "youtube#playlist" -> "https://www.youtube.com/playlist?list=" + idObject["playlistId"].asText()
            else -> throw RuntimeException()
        }
    }

    override fun getMessage(event: SlashCommandInteractionEvent, items: JsonNode, index: Int): MessageEditData {
        return MessageEdit(
            content = "Video ${index + 1}/${items.size()} for query \"${
                event.getOption("query")?.asString
            }\": ${getUrl(items[index])}"
        )

    }

    override val commandData: SlashCommandData
        get() = Commands.slash("youtube", "Search youtube")
            .addOption(OptionType.STRING, "query", "Search query", true)
            .addOptions(
                OptionData(OptionType.STRING, "type", "Type of resource. Default: video", false)
                    .addChoices("video" to "video", "playlist" to "playlist", "channel" to "channel"),
                OptionData(OptionType.INTEGER, "index", "Video index", false)
                    .setRequiredRange(0, 49),
                OptionData(OptionType.INTEGER, "max", "Maximum result count", false)
                    .setRequiredRange(1, 50)
            )
}