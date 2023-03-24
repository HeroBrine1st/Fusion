package ru.herobrine1st.fusion.module.vk.command

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.database.applicationDatabase
import ru.herobrine1st.fusion.database.await
import ru.herobrine1st.fusion.database.awaitAsOne
import ru.herobrine1st.fusion.module.vk.VkGroup
import ru.herobrine1st.fusion.module.vk.exceptions.VkApiException
import ru.herobrine1st.fusion.module.vk.util.VkApiUtil
import java.util.regex.Pattern

private val logger = LoggerFactory.getLogger(SubscribeSubcommand::class.java)

object SubscribeSubcommand {
    const val URL_ARGUMENT = "url"

    private val pattern = Pattern.compile("(?:https?://)?vk\\.com/(?:(?:club|public)(\\d+)|([^/]+))")

    suspend fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply(true).await()
        val url = event.getOption(URL_ARGUMENT) { it.asString }!! // Required on discord side
        val matcher = pattern.matcher(url)
        if (!matcher.matches()) {
            event.hook.sendMessage("Invalid URL provided").await()
            return
        }
        if (event.guild == null) {
            event.hook.sendMessage("This feature works only in guilds")
            return
        }
        applicationDatabase.vkChannelSubscriptionQueries.getChannelSubscriptionsCount(event.channel.idLong)
            .awaitAsOne()
            .let { subscribesCount ->
                if (subscribesCount >= 25) {
                    event.hook.sendMessage("This channel has reached 25 subscriptions limit").await()
                    return@execute
                }
            }

        val group = try {
            VkApiUtil.getGroupById(matcher.group(2) ?: matcher.group(1)!!)
        } catch (e: VkApiException) {
            if (e.code == 100) {
                event.hook.sendMessage("No such group").await()
                return
            } else {
                throw e
            }
        }
        if (group.isClosed) {
            event.hook.sendMessage("Group is closed").await()
            return
        }
        val entity: VkGroup = try {
            applicationDatabase.vkGroupQueries.create(
                groupId = group.id,
                lastWallPostId = -1,
                name = group.name,
                avatarUrl = group.photo_200
            ).awaitAsOne()
        } catch (t: Throwable) {
            logger.error("Couldn't add group to database", t)
            event.hook.sendMessage("An unknown error occurred while adding group to database").await()
            return
        }

        if (entity.lastWallPostId == -1) {
            val firstPost = try {
                VkApiUtil.getWall(-group.id).filterNot { it.isPinned }.firstOrNull()
            } catch(t: Throwable) {
                logger.error("Couldn't fetch vk wall", t)
                event.hook.sendMessage("An unknown error occurred while fetching wall").await()
                return
            }
            if (firstPost != null) try {
                applicationDatabase.vkGroupQueries.updateLastWallPostId(
                    groupId = group.id,
                    lastWallPostId = firstPost.id
                ).await()
            } catch(t: Throwable) {
                logger.error("Couldn't update lastWallPostId in database", t)
                event.hook.sendMessage("An unknown error occurred while adding group to database").await()
                return
            }
        }
        try {
            applicationDatabase.vkChannelSubscriptionQueries.create(
                groupId = group.id,
                channelId = event.channel.idLong,
                guildId = event.guild!!.idLong
            ).await()
        } catch (t: Throwable) {
            logger.error("Couldn't add subscription to database", t)
            event.hook.sendMessage("An unknown error occurred while adding subscription to database").await()
            return
        }

        event.hook.sendMessage(
            MessageCreate(
                embeds = listOf(
                    Embed(
                        color = 0x00FF00,
                        authorName = entity.name,
                        authorUrl = "https://vk.com/club" + entity.id,
                        authorIcon = entity.avatarUrl,
                        description = "Complete. This message is an example post."
                    )
                )
            )
        ).await()
    }
}