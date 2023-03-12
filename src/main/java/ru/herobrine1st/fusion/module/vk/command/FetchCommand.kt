package ru.herobrine1st.fusion.module.vk.command

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import ru.herobrine1st.fusion.listener.awaitInteraction
import ru.herobrine1st.fusion.module.vk.exceptions.VkApiException
import ru.herobrine1st.fusion.module.vk.model.Post
import ru.herobrine1st.fusion.module.vk.util.VkApiUtil
import ru.herobrine1st.fusion.module.vk.util.toEmbeds
import java.util.regex.Pattern

object FetchCommand {
    const val URL_ARGUMENT = "url"
    const val EPHEMERAL_ARGUMENT = "ephemeral"

    private val pattern = Pattern.compile("""(?:https?://)?vk\.com/(?:[^?]+\?w=)?wall(-?\d+)_(\d+)""")

    suspend fun execute(event: SlashCommandInteractionEvent) {
        val url = event.getOption(URL_ARGUMENT)!!.asString // Required on discord side
        val matcher = pattern.matcher(url)
        if (!matcher.matches()) {
            event.reply("Invalid URL provided").setEphemeral(true).await()
            return
        }
        val ephemeral = event.getOption(EPHEMERAL_ARGUMENT)?.asBoolean ?: true
        event.deferReply(ephemeral).await()
        val wallId = matcher.group(1).toInt()
        val postId = matcher.group(2)
        val (wallName, wallAvatarUrl) = run {
            if (wallId > 0) {
                val (user) = VkApiUtil.getUsersById(wallId.toString()).ifEmpty {
                    event.hook.sendMessage("Post is not found").await()
                    return
                }
                "${user.firstName} ${user.lastName}" to user.photo_200
            } else {
                val group = VkApiUtil.getGroupById((-wallId).toString())
                group.name to group.photo_200
            }
        }
        val post: Post

        try {
            post = VkApiUtil.getPostsById("${wallId}_$postId").ifEmpty {
                event.hook.sendMessage("Post is not found").await()
                return
            }[0]
        } catch (e: VkApiException) {
            if (e.code == 15) {
                event.hook.sendMessage("Cannot access this post")
            } else {
                event.hook.sendMessage("Unknown error occurred")
            }
            e.printStackTrace()
            return
        }
        val message: Message = event.hook.sendMessageEmbeds(post.toEmbeds(wallName, wallAvatarUrl))
            .apply {
                if (!ephemeral) {
                    setComponents(ActionRow.of(Button.danger("delete", "Delete")))
                }
            }
            .await()
        if (ephemeral) return

        while (true) {
            val interaction = message.awaitInteraction()
            if (interaction.user.id != event.user.id) {
                interaction.reply("You can not delete this message because you're not who called the command")
                    .setEphemeral(true)
                    .await()
                continue
            }
            message.delete().await()
            break
        }
    }
}