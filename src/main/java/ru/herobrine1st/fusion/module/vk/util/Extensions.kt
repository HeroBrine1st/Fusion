package ru.herobrine1st.fusion.module.vk.util

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import ru.herobrine1st.fusion.module.vk.model.Link
import ru.herobrine1st.fusion.module.vk.model.Photo
import ru.herobrine1st.fusion.module.vk.model.Post
import ru.herobrine1st.fusion.util.ModifiedEmbedBuilder
import kotlin.math.min

fun Photo.getLargestSize(): Photo.Size {
    return sizes.maxByOrNull { it.width }!!
}


fun Post.toEmbeds(wallName: String, wallAvatarUrl: String?, repost: Boolean = false): List<MessageEmbed> {
    if (copyHistory.isNotEmpty()) return copyHistory.first().toEmbeds(wallName, wallAvatarUrl, true)

    val text = text.replace(Regex("""\[([^|]+)\|([^]]+)]""")) {
        "[${it.groupValues[2]}](${
            if(it.groupValues[1].startsWith("http")) it.groupValues[1]
            else "https://vk.com/" + it.groupValues[1]
        })"
    }

    val url = "https://vk.com/wall${ownerId}_$id"
    val embedBuilder = ModifiedEmbedBuilder()
        .setTitle(null, url)
        .setAuthor(wallName, url, wallAvatarUrl)
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
                    EmbedBuilder()
                        .apply {
                            if (attachment.title?.isNotBlank() == true)
                                setTitle(attachment.title, attachment.url)
                            setDescription(attachment.description)
                            setFooter(attachment.caption)
                            setImage(attachment.photo?.getLargestSize()?.url)
                        }.let {
                            if (!it.isEmpty) embeds.add(it.build())
                        }
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