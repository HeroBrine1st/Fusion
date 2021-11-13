package ru.herobrine1st.fusion.net;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import ru.herobrine1st.fusion.Pools;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;


public class JsonRequest {
    public static record JsonResponse(JsonObject responseJson, Response response) {
    }

    private JsonRequest() {
    }

    private static final Gson gson = new Gson();
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient();

    public static @NotNull CompletableFuture<JsonResponse> makeRequest(HttpUrl url) {
        return makeRequest(url, null, "GET");
    }

    public static @NotNull CompletableFuture<JsonResponse> makeRequest(HttpUrl url, @Nullable JsonObject data) {
        return makeRequest(url, data, "POST");
    }

    public static @NotNull CompletableFuture<JsonResponse> makeRequest(HttpUrl url, @Nullable JsonObject data, @NotNull String method) {
        CompletableFuture<JsonResponse> completableFuture = new CompletableFuture<>();
        Pools.CONNECTION_POOL.execute(() -> {
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json");
            builder.method(method, data == null ? null : RequestBody.create(data.toString(), MEDIA_TYPE));
            Request request = builder.build();
            Response response;
            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
                completableFuture.completeExceptionally(e);
                return;
            }
            ResponseBody body = response.body();
            if (body == null) {
                completableFuture.completeExceptionally(new NullPointerException("Body is null"));
                return;
            }
            String bodyString;
            try {
                bodyString = body.string();
            } catch (IOException e) {
                completableFuture.completeExceptionally(e);
                return;
            }
            JsonObject json;
            try {
                json = gson.fromJson(bodyString, JsonObject.class);
            } catch (JsonSyntaxException e) {
                completableFuture.completeExceptionally(e);
                return;
            }
            completableFuture.complete(new JsonResponse(json, response));
        });
        return completableFuture;
    }
}
