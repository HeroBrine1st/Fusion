package ru.herobrine1st.fusion.module.vk.task

import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.jda
import ru.herobrine1st.fusion.module.vk.entity.VkGroupEntity
import ru.herobrine1st.fusion.module.vk.entity.VkGroupSubscriberEntity
import ru.herobrine1st.fusion.module.vk.exceptions.VkApiException
import ru.herobrine1st.fusion.module.vk.util.VkApiUtil
import ru.herobrine1st.fusion.module.vk.util.toEmbeds
import ru.herobrine1st.fusion.sessionFactory
import ru.herobrine1st.fusion.util.scheduleAtFixedRate
import java.util.concurrent.TimeUnit

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
                .asReversed()
                .forEach { post ->
                    val embeds = post.toEmbeds(group.name, group.avatarUrl)
                    group.lastWallPostId = post.id.toLong()
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

