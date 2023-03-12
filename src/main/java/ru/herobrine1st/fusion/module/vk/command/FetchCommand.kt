package ru.herobrine1st.fusion.module.vk.command

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import ru.herobrine1st.fusion.module.vk.exceptions.VkApiException
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
        event.deferReply(event.getOption(EPHEMERAL_ARGUMENT)?.asBoolean ?: true).await()
        val wallId = matcher.group(1).toInt()
        val postId = matcher.group(2)
        val (wallName, wallAvatarUrl) = run {
            if (wallId > 0) {
                val (user) = VkApiUtil.getUsersById(wallId.toString()).ifEmpty {
                    event.hook.sendMessage("Post is not found")
                    return
                }
                "${user.firstName} ${user.lastName}" to user.photo_200
            } else {
                val group = VkApiUtil.getGroupById((-wallId).toString())
                group.name to group.photo_200
            }
        }
        val (post) = VkApiUtil.getPostsById("${wallId}_$postId").ifEmpty {
            event.hook.sendMessage("Post is not found")
            return
        }
        try {
            event.hook.sendMessageEmbeds(post.toEmbeds(wallName, wallAvatarUrl)).await()
        } catch (e: VkApiException) {
            if (e.code == 15) {
                event.hook.sendMessage("Cannot access this post")
            } else {
                event.hook.sendMessage("Unknown error occurred")
            }
            e.printStackTrace()
        }
    }
}