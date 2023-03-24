package ru.herobrine1st.fusion.module.vk.command

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.messages.MessageCreate
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.database.applicationDatabase
import ru.herobrine1st.fusion.database.awaitAsList
import ru.herobrine1st.fusion.database.awaitAsOne
import ru.herobrine1st.fusion.listener.awaitInteraction
import ru.herobrine1st.fusion.util.abbreviate

private val logger = LoggerFactory.getLogger(UnsubscribeSubcommand::class.java)

object UnsubscribeSubcommand {
    suspend fun execute(event: SlashCommandInteractionEvent) {
        event.deferReply(true).await()
        val groups =
            applicationDatabase.vkChannelSubscriptionQueries.getSubscribedGroups(event.channel.idLong).awaitAsList()
        if (groups.isEmpty()) {
            event.hook.sendMessage("There are no subscriptions in this channel").await()
            return
        }
        val message = event.hook.sendMessage(
            MessageCreate(
                content = "Select groups to unsubscribe",
                components = listOf(
                    ActionRow.of(
                        StringSelectMenu(
                            customId = "groups",
                            placeholder = "Select groups to unsubscribe",
                            options = groups.map {
                                SelectOption.of(it.name.abbreviate(100), it.id.toString())
                            })
                    )
                )
            )
        ).await()
        val selectMenuInteractionEvent = message.awaitInteraction()
        selectMenuInteractionEvent.deferEdit().await()
        if (selectMenuInteractionEvent !is StringSelectInteractionEvent)
            throw RuntimeException("Unexpected event: ${selectMenuInteractionEvent::class.java}")
        val selectedSubscriptions = selectMenuInteractionEvent.selectedOptions.map { it.value.toLong() }
        val deletedCount = try {
            applicationDatabase.vkChannelSubscriptionQueries.unsubscribe(selectedSubscriptions)
                .awaitAsOne()
        } catch(t: Throwable) {
            logger.error("Couldn't delete subscription data from database", t)
            selectMenuInteractionEvent.hook.editOriginal("An unknown error occurred").await()
            return
        }

        if (deletedCount > 0) selectMenuInteractionEvent.hook.editOriginal("Success ($deletedCount/${selectedSubscriptions.size})")
            .await()
        else selectMenuInteractionEvent.hook.editOriginal("No changes occurred because there's no subscription to any of the selected groups")
            .await()
    }
}