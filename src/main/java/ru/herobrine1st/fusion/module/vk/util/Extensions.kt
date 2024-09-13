package ru.herobrine1st.fusion.module.vk.util

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import ru.herobrine1st.fusion.module.vk.model.*
import ru.herobrine1st.fusion.util.ModifiedEmbedBuilder
import kotlin.math.min

fun Photo.getLargestSize(): Photo.Size? {
    return sizes.maxByOrNull { it.width }
}

private const val maxImagesPerEmbed = 4

fun Post.toEmbeds(wallName: String, wallAvatarUrl: String?, repost: Boolean = false): List<MessageEmbed> {
    if (copyHistory.isNotEmpty()) return copyHistory.first().toEmbeds(wallName, wallAvatarUrl, true)

    val text = text.replace(Regex("""\[([^|]+)\|([^]]+)]""")) {
        "[${it.groupValues[2]}](${
            if (it.groupValues[1].startsWith("http")) {
                it.groupValues[1]
            } else {
                "https://vk.com/" + it.groupValues[1]
            }
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
        if (attachments.any { it !is Photo && it !is Link && it !is Document && it.isNonGifDocument() }) {
            footerBuilder.append("Post contains incompatible attachments\n")
        }

        attachments.mapNotNull {
            when {
                it is Photo -> it.getLargestSize()?.url
                it is Document && it.type == Document.Type.Gif -> it.url
                else -> null
            }
        }.let { urls: List<String> ->
            if (urls.isEmpty()) return@let
            embedBuilder.setImage(urls.first())
            urls.subList(1, min(maxImagesPerEmbed, urls.size)).forEach {
                embeds.add(
                    ModifiedEmbedBuilder()
                        .setTitle(null, it)
                        .setImage(it)
                        .build()
                )
            }
            if (urls.size > maxImagesPerEmbed) footerBuilder.append("Post contains more that 4 gif or photo\n")
        }

        attachments.filterIsInstance<Link>()
            .mapNotNull { attachment ->
                EmbedBuilder().apply {
                    attachment.title?.takeIf { it.isNotBlank() }?.let { setTitle(it, attachment.url) }
                    setDescription(attachment.description)
                    setFooter(attachment.caption)
                    setImage(attachment.photo?.getLargestSize()?.url)
                }.takeIf { !it.isEmpty }?.build()
            }.let { embeds.addAll(it) }

    }
    if (repost) {
        footerBuilder.append("This post is a repost\n")
    }
    embeds.add(
        0, embedBuilder
            .setFooter(footerBuilder.toString())
            .build()
    )
    return embeds
}

private fun Any.isNonGifDocument(): Boolean =
    this is Document && type != Document.Type.Gif