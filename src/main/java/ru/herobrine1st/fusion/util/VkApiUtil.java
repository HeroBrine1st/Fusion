package ru.herobrine1st.fusion.util;

import okhttp3.HttpUrl;
import ru.herobrine1st.fusion.Config;

import java.util.Objects;

public class VkApiUtil {
    public static HttpUrl.Builder getHttpUrlBuilder(String method) {
        return Objects.requireNonNull(HttpUrl.parse("https://api.vk.com/method/" + method)).newBuilder()
                .addQueryParameter("access_token", Config.getVkServiceToken())
                .addQueryParameter("v", Config.getVkAPIVersion());
    }
}
