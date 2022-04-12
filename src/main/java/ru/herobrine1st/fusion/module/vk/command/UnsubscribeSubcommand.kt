package ru.herobrine1st.fusion.module.vk.command

import dev.minn.jda.ktx.Message
import dev.minn.jda.ktx.await
import dev.minn.jda.ktx.interactions.SelectMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.listener.awaitInteraction
import ru.herobrine1st.fusion.module.vk.entity.VkGroupSubscriberEntity
import ru.herobrine1st.fusion.sessionFactory
import ru.herobrine1st.fusion.util.abbreviate

private val logger = LoggerFactory.getLogger(UnsubscribeSubcommand::class.java)

object UnsubscribeSubcommand {
    suspend fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply(true).await()
        val groups = withContext(Dispatchers.IO) {
            sessionFactory.openSession().use { session ->
                val query = session.createQuery(
                    "SELECT entity FROM VkGroupSubscriberEntity entity " +
                            "JOIN FETCH entity.group " +
                            "WHERE entity.channelId=:channelId",
                    VkGroupSubscriberEntity::class.java
                ).setParameter(
                    "channelId",
                    event.channel.idLong
                )
                return@withContext query.resultList
            }
        }
        if (groups.isEmpty()) {
            event.hook.sendMessage("There are no subscriptions in this channel").await()
            return
        }
        val message = event.hook.sendMessage(
            Message {
                content = "Select groups to unsubscribe"
                builder.setActionRows(
                    ActionRow.of(
                        SelectMenu(
                            customId = "groups",
                            placeholder = "Select groups to unsubscribe",
                            options = groups.map {
                                SelectOption.of(it.group.name.abbreviate(100), it.id.toString())
                            })
                    )
                )
            }
        ).await()
        val selectMenuInteractionEvent = message.awaitInteraction()
        selectMenuInteractionEvent.deferEdit().await()
        if (selectMenuInteractionEvent !is SelectMenuInteractionEvent)
            throw RuntimeException("Unexpected event: ${selectMenuInteractionEvent::class.java}")
        val selectedGroups = selectMenuInteractionEvent.selectedOptions.map { it.value.toInt() }.joinToString(",")
        logger.debug("Selected groups: $selectedGroups")
        val success = withContext(Dispatchers.IO) {
            sessionFactory.openSession().use { session ->
                val transaction = session.beginTransaction()
                try {
                    session.createMutationQuery(
                        "DELETE FROM VkGroupSubscriberEntity entity " +
                                "WHERE entity.id IN :ids AND entity.channelId=:channelId"
                    )
                        .setParameter("ids", selectedGroups)
                        .setParameter("channelId", event.channel.idLong)
                        .executeUpdate()
                    transaction.commit()
                    true
                } catch (e: Exception) {
                    transaction.rollback()
                    logger.error("An error occurred while unsubscribing channel", e)
                    selectMenuInteractionEvent.hook.editOriginal("Error").await()
                    false
                }
            }
        }
        if (success) selectMenuInteractionEvent.hook.editOriginal("Success").await()
    }
}