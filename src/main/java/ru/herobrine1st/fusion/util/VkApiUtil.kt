package ru.herobrine1st.fusion.util

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import ru.herobrine1st.fusion.Config
import java.util.*

object VkApiUtil {
    fun getHttpUrlBuilder(method: String): HttpUrl.Builder {
        return Objects.requireNonNull("https://api.vk.com/method/$method".toHttpUrl()).newBuilder()
            .addQueryParameter("access_token", Config.vkServiceToken)
            .addQueryParameter("v", Config.vkAPIVersion)
    }
}