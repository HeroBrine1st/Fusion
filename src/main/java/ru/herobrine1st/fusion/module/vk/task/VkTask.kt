package ru.herobrine1st.fusion.module.vk.task

import dev.minn.jda.ktx.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.jda
import ru.herobrine1st.fusion.module.vk.entity.VkGroupEntity
import ru.herobrine1st.fusion.module.vk.entity.VkGroupSubscriberEntity
import ru.herobrine1st.fusion.module.vk.exceptions.VkApiException
import ru.herobrine1st.fusion.module.vk.model.Link
import ru.herobrine1st.fusion.module.vk.model.Photo
import ru.herobrine1st.fusion.module.vk.model.Post
import ru.herobrine1st.fusion.module.vk.util.VkApiUtil
import ru.herobrine1st.fusion.module.vk.util.getLargestSize
import ru.herobrine1st.fusion.sessionFactory
import ru.herobrine1st.fusion.util.ModifiedEmbedBuilder
import ru.herobrine1st.fusion.util.scheduleAtFixedRate
import java.util.concurrent.TimeUnit
import kotlin.math.min

private val logger = LoggerFactory.getLogger("VkTask")

fun registerVkTask() {
    scheduleAtFixedRate(
        initialDelay = 1,
        period = 30,
        unit = TimeUnit.MINUTES
    ) {
        val groups = withContext(Dispatchers.IO) {
            sessionFactory.openSession().use { session ->
                return@withContext session
                    .createQuery(
                        "SELECT entity FROM VkGroupEntity entity " +
                                "JOIN FETCH entity.subscribers " +
                                "WHERE entity.subscribers IS NOT EMPTY",
                        VkGroupEntity::class.java
                    ).resultList
            }
        }
        val pendingRemove = mutableListOf<VkGroupSubscriberEntity>()
        for (group in groups) {
            try {
                VkApiUtil.getWall(-group.groupId)
            } catch (e: VkApiException) {
                logger.error("Error fetching group ${group.name} (${group.groupId})", e)
                continue
            }
                .filterNot { it.isPinned }
                .filter { it.id > group.lastWallPostId }
                .reversed()
                .forEach {
                    val (post, repost) = if (it.copyHistory.isNotEmpty()) {
                        it.copyHistory.first() to true
                    } else it to false
                    val embeds = post.toEmbeds(group, repost)
                    group.lastWallPostId = it.id.toLong()
                    for (subscriber in group.subscribers) {
                        fun removeFromDatabase(subscriber: VkGroupSubscriberEntity) {
                            logger.info(
                                "Cannot send message to channel ${subscriber.channelId} in guild ${subscriber.guildId} " +
                                        "- removing from subscriptions"
                            )
                            pendingRemove.add(subscriber)
                        }
                        try {
                            jda.getGuildById(subscriber.guildId)
                                ?.getTextChannelById(subscriber.channelId)
                                ?.sendMessageEmbeds(embeds)?.await() ?: removeFromDatabase(subscriber)
                        } catch (e: InsufficientPermissionException) {
                            removeFromDatabase(subscriber)
                        }
                    }
                }
        }
        withContext(Dispatchers.IO) {
            sessionFactory.openSession().use { session ->
                val transaction = session.beginTransaction()
                try {
                    pendingRemove.forEach { session.remove(it) }
                    groups.forEach { session.merge(it) }
                    transaction.commit()
                } catch (e: Exception) {
                    logger.error(
                        "Cannot remove invalid subscriptions (${pendingRemove.joinToString(" ") { it.id.toString() }}) from database",
                        e
                    )
                    transaction.rollback()
                }
            }
        }
    }
}

fun Post.toEmbeds(group: VkGroupEntity, repost: Boolean = false): List<MessageEmbed> {
    val url = "https://vk.com/club${ownerId}?w=wall-${ownerId}_$id"
    val embedBuilder = ModifiedEmbedBuilder()
        .setTitle(null, url)
        .setAuthor(group.name, url, group.avatarUrl)
        .setTimestamp(date)
        .setDescription(
            if (text.length > 2048) {
                val additionalText = "... Post is too big (${text.length}/2048 symbols)"
                text.substring(0, 2048 - additionalText.length) + additionalText
            } else text
        )
    val footerBuilder = StringBuilder()
    val embeds: MutableList<MessageEmbed> = ArrayList()
    if (attachments.isNotEmpty()) {
        if (attachments.any { it !is Photo && it !is Link }) {
            footerBuilder.append("Post contains incompatible attachments\n")
        }
        with(attachments.filterIsInstance<Photo>()) {
            if (isEmpty()) return@with
            embedBuilder.setImage(this[0].getLargestSize().url)
            this.subList(1, min(4, size)).forEach {
                embeds.add(
                    ModifiedEmbedBuilder()
                        .setTitle(null, url)
                        .setImage(it.getLargestSize().url)
                        .build()
                )
            }
            if (size > 4) footerBuilder.append("Post contains more than 4 images\n")
        }
        for (attachment in attachments) {
            when (attachment) {
                is Link -> {
                    embeds.add(
                        EmbedBuilder()
                            .setTitle(attachment.title, attachment.url)
                            .setDescription(attachment.description)
                            .setFooter(attachment.caption)
                            .setImage(attachment.photo?.getLargestSize()?.url).build()
                    )
                }
                else -> {
                    // no support
                }
            }
        }
    }
    if (repost)
        footerBuilder.append("This post is a repost\n")
    embeds.add(
        0, embedBuilder
            .setFooter(footerBuilder.toString())
            .build()
    )
    return embeds
}