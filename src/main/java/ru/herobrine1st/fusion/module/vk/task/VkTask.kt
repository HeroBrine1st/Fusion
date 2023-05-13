package ru.herobrine1st.fusion.module.vk.task

import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.database.Database
import ru.herobrine1st.fusion.database.applicationDatabase
import ru.herobrine1st.fusion.database.await
import ru.herobrine1st.fusion.database.awaitAsList
import ru.herobrine1st.fusion.jda
import ru.herobrine1st.fusion.module.vk.VkChannelSubscription
import ru.herobrine1st.fusion.module.vk.exceptions.VkApiException
import ru.herobrine1st.fusion.module.vk.util.VkApiUtil
import ru.herobrine1st.fusion.module.vk.util.toEmbeds
import ru.herobrine1st.fusion.util.scheduleAtFixedRate
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("VkTask")

fun registerVkTask() {
    scheduleAtFixedRate(
        initialDelay = 0,
        period = 30,
        unit = TimeUnit.MINUTES
    ) {
        logger.trace("Fetching subscriptions")
        val groups = applicationDatabase.vkGroupQueries.getAllWithSubscribers().awaitAsList()
        logger.trace("There's ${groups.size} groups with subscribers")
        val invalidSubscriptions = mutableListOf<Long>()

        fun removeFromDatabase(subscriber: VkChannelSubscription) {
            logger.info(
                "Cannot send message to channel ${subscriber.channelId} in guild ${subscriber.guildId} " +
                        "- removing ${subscriber.id} from subscriptions"
            )
            invalidSubscriptions.add(subscriber.id)
        }

        for (group in groups) {
            logger.trace("Fetching group ${group.id} (${group.name}), last post id ${group.lastWallPostId}")
            try {
                VkApiUtil.getWall(-group.groupId)
            } catch (e: VkApiException) {
                logger.error("Error fetching group ${group.name} (${group.groupId})", e)
                continue
            }
                .filterNot { it.isPinned }
                .filter { it.id > group.lastWallPostId }
                .asReversed()
                .also {
                    logger.trace("Sending ${it.size} posts")
                }
                .takeWhile { post ->
                    val embeds = post.toEmbeds(group.name, group.avatarUrl)
                    val subscribers =
                        applicationDatabase.vkChannelSubscriptionQueries.getGroupSubscriptions(group.groupId)
                            .awaitAsList()
                    val jobs = supervisorScope {
                        subscribers.map { subscriber ->
                            launch {
                                try {
                                    jda.getGuildById(subscriber.guildId)
                                        ?.getTextChannelById(subscriber.channelId)
                                        ?.sendMessageEmbeds(embeds)?.await() ?: removeFromDatabase(subscriber)
                                } catch (e: InsufficientPermissionException) {
                                    removeFromDatabase(subscriber)
                                } catch (t: Throwable) {
                                    logger.error(
                                        "An error occurred while sending post ${post.id} " +
                                                "to channel ${subscriber.channelId} in guild ${subscriber.guildId}"
                                    )
                                    throw t
                                }
                            }
                        }
                    }
                    // can't send post anywhere => break
                    jobs.any { !it.isCancelled }
                }
                .lastOrNull()?.let {
                    logger.trace("Updating lastWallPostId to ${it.id}")
                    applicationDatabase.vkGroupQueries.updateLastWallPostId(
                        groupId = group.groupId,
                        lastWallPostId = it.id
                    ).await()
                }
        }
        if (invalidSubscriptions.isNotEmpty()) withContext(Dispatchers.Database) {
            logger.info("Doing actual deletion of subscriptions ${invalidSubscriptions.joinToString(",")}")
            applicationDatabase.vkChannelSubscriptionQueries.unsubscribe(invalidSubscriptions)
        }
    }
}

