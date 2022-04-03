package ru.herobrine1st.fusion

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import ru.herobrine1st.fusion.module.vk.model.Post
import ru.herobrine1st.fusion.module.vk.util.VkApiUtil
import ru.herobrine1st.fusion.util.objectMapper

class TestVkPostModel {
    companion object {
        @JvmStatic
        lateinit var vkToken: String

        @BeforeAll
        @JvmStatic
        fun before() {
            vkToken = System.getenv("VK_TOKEN")
        }
    }

    private val okHttpClient = OkHttpClient()


    @Test
    fun test() {
        okHttpClient.newCall(
            Request.Builder()
                .url(VkApiUtil.getHttpUrlBuilder("wall.getById", token = vkToken)
                    .addQueryParameter("posts", "-57536014_9408854")
                    .build().also { println(it.toString()) })
                .build()
        ).execute().use {
            objectMapper.readerFor(jacksonTypeRef<List<Post>>()).with(DeserializationFeature.UNWRAP_ROOT_VALUE)
                .withRootName("response")
                .readValue<List<Post>>(it.body!!.charStream())
        }
    }
}