package ru.herobrine1st.fusion.listener

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalNotification
import dev.minn.jda.ktx.await
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.Config
import java.time.Duration
import java.util.concurrent.CompletableFuture

typealias InteractionFuture = CompletableFuture<GenericComponentInteractionCreateEvent>

object ButtonInteractionListener : EventListener {
    private val logger: Logger = LoggerFactory.getLogger(ButtonInteractionListener::class.java)

    // Maybe wrong semantics, just googled about it
    private val interactionCache: Cache<Long, InteractionFuture> = CacheBuilder.newBuilder()
        .apply { Config.maxComponentInteractionWaits?.let { maximumSize(it) } }
        .expireAfterWrite(Duration.ofMinutes(Config.maxComponentInteractionWaitTimeMinutes ?: 15))
        .removalListener { it: RemovalNotification<Long, InteractionFuture> ->
            logger.trace("Key ${it.value} removed from interaction cache")
            it.value?.cancel(true)
        }.build()

    fun createInteractionFuture(messageId: Long): InteractionFuture {
        assert(!interactionCache.asMap().containsKey(messageId))
        val future = InteractionFuture()
        interactionCache.put(messageId, future)
        logger.debug("Waiting for interaction in $messageId")
        return future
    }

    override fun onEvent(event: GenericEvent) {
        if (event !is GenericComponentInteractionCreateEvent) return
        val entry = interactionCache.getIfPresent(event.messageIdLong)
        if (entry == null) {
            event.reply("This message no longer accepts component interactions.").setEphemeral(true).queue()
            return
        }
        logger.debug("Got button click on ${event.messageIdLong}")
        entry.complete(event)
        interactionCache.invalidate(event)
    }
}

suspend fun Message.awaitInteraction(): GenericComponentInteractionCreateEvent {
    if(actionRows.isEmpty() || actionRows.any { it.isEmpty }) throw RuntimeException("No action rows in message")
    return ButtonInteractionListener.createInteractionFuture(idLong).await()
}