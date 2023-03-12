package ru.herobrine1st.fusion.module.vk.command

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import jakarta.persistence.NoResultException
import jakarta.persistence.TypedQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.hibernate.Transaction
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.module.vk.entity.VkGroupEntity
import ru.herobrine1st.fusion.module.vk.entity.VkGroupSubscriberEntity
import ru.herobrine1st.fusion.module.vk.exceptions.VkApiException
import ru.herobrine1st.fusion.module.vk.util.VkApiUtil
import ru.herobrine1st.fusion.sessionFactory
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

        withContext(Dispatchers.IO) {
            sessionFactory.openSession().use { session ->
                val query: TypedQuery<Long> = session.createQuery(
                    "SELECT count(entity) FROM VkGroupSubscriberEntity entity " +
                            "WHERE entity.channelId=:channelId", Long::class.javaObjectType
                ).setParameter(
                    "channelId",
                    event.channel.idLong
                )
                return@withContext query.singleResult
            }
        }.let { subscribesCount ->
            if (subscribesCount >= 25) {
                event.hook.sendMessage("This channel has reached 25 subscriptions limit").await()
                return@execute
            }
        }

        val group = try {
            VkApiUtil.getGroupById(matcher.group(2) ?: matcher.group(1)!!)
        } catch (e: VkApiException) {
            if(e.code == 100) {
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
        val id = group.id
        val entity: VkGroupEntity = withContext(Dispatchers.IO) {
            sessionFactory.openSession().use { session ->
                val query: TypedQuery<VkGroupEntity> = session.createQuery(
                    "SELECT entity FROM VkGroupEntity entity " +
                            "WHERE entity.groupId=:id", VkGroupEntity::class.java
                )
                    .setParameter("id", id.toLong())
                try {
                    query.singleResult
                } catch (exception: NoResultException) {
                    VkGroupEntity().apply {
                        this.groupId = id.toLong()
                        lastWallPostId = -1L
                        originalLink = url
                    }
                }
            }
        }
        entity.name = group.name
        entity.avatarUrl = group.photo_200
        if (entity.lastWallPostId == -1L) {
            val firstPost = VkApiUtil.getWall(-entity.groupId).filterNot { it.isPinned }.first()
            entity.lastWallPostId = firstPost.id.toLong()
            logger.debug("lastWallPostId: ${entity.lastWallPostId}")
        }

        withContext(Dispatchers.IO) {
            val vkGroupSubscriber = VkGroupSubscriberEntity().apply {
                this.group = entity
                channelId = event.channel.idLong
                guildId = event.guild!!.idLong
            }

            sessionFactory.openSession().use { session ->
                val transaction: Transaction = session.beginTransaction()
                try {
                    session.merge(entity) // TODO change to persist?
                    session.persist(vkGroupSubscriber)
                    transaction.commit()
                } catch (e: Exception) {
                    transaction.rollback()
                    throw RuntimeException(e)
                }
            }
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