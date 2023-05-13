package ru.herobrine1st.fusion.module.spotify

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import kotlinx.coroutines.delay
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import ru.herobrine1st.fusion.jda

val urlRegex = Regex("""https?://open\.spotify\.com/.+""")
const val maxTimeMs = 30000
fun registerSpotifyListener() {
    jda.listener<MessageReceivedEvent> { event ->
        if (urlRegex.containsMatchIn(event.message.contentRaw)) {
            var embeds: List<MessageEmbed> = event.message.embeds.filter {
                it.url?.matches(urlRegex) == true
            }
            if (embeds.isEmpty()) {
                val startTime = System.currentTimeMillis()
                var waitTime = 625L
                while (System.currentTimeMillis() - startTime <= maxTimeMs) {
                    val localEmbeds = event.channel.asTextChannel()
                        .retrieveMessageById(event.messageId)
                        .await()
                        .embeds.filter {
                            it.url?.matches(urlRegex) == true
                        }
                    if (localEmbeds.isEmpty()) {
                        delay(waitTime)
                        waitTime = (waitTime * 1.5).toLong()
                        continue
                    }
                    embeds = localEmbeds
                    break
                }
                if(embeds.isEmpty()) return@listener
            }

            event.message.replyEmbeds(embeds)
                .mentionRepliedUser(false)
                .await()
        }
    }
}