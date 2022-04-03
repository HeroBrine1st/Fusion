package ru.herobrine1st.fusion.module.vk.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.treeToValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.Config
import ru.herobrine1st.fusion.module.vk.exceptions.VkApiException
import ru.herobrine1st.fusion.module.vk.model.Group
import ru.herobrine1st.fusion.module.vk.model.Post
import ru.herobrine1st.fusion.module.vk.model.VkApiError
import ru.herobrine1st.fusion.util.objectMapper

object VkApiUtil {
    private val logger = LoggerFactory.getLogger(VkApiUtil::class.java)
    private val okHttpClient = OkHttpClient()

    fun getHttpUrlBuilder(method: String, token: String = Config.vkServiceToken): HttpUrl.Builder {
        return "https://api.vk.com/method/$method".toHttpUrl().newBuilder()
            .addQueryParameter("access_token", token)
            .addQueryParameter("v", Config.vkAPIVersion)
    }

    suspend fun executeMethod(url: HttpUrl): JsonNode {
        return withContext(Dispatchers.IO) {
            okHttpClient.newCall(
                Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .build()
            ).execute().use {
                val node: ObjectNode = objectMapper.readValue(it.body!!.charStream())
                if (node.has("error")) {
                    with(objectMapper.treeToValue<VkApiError>(node.get("error"))) {
                        logger.error("Error executing vk method ($errorCode $errorMsg)")
                        if (logger.isDebugEnabled) logger.debug(node.toPrettyString())
                        throw VkApiException(errorCode, errorMsg)
                    }
                } else {
                    return@withContext node.get("response")
                }
            }
        }
    }

    suspend fun getGroupById(id: String): Group {
        return objectMapper.treeToValue<List<Group>>(
            executeMethod(
                getHttpUrlBuilder("groups.getById")
                    .addQueryParameter("group_id", id)
                    .build()
            )
        )[0]
    }

    suspend fun getWall(ownerId: Long): List<Post> {
        return objectMapper.treeToValue(
            executeMethod(
                getHttpUrlBuilder("wall.get")
                    .setQueryParameter("owner_id", ownerId.toString())
                    .build()
            ).get("items")
        )
    }
}