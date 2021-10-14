package ru.herobrine1st.fusion.module.googlesearch.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import ru.herobrine1st.fusion.api.Fusion;
import ru.herobrine1st.fusion.api.exception.CommandException;
import ru.herobrine1st.fusion.api.restaction.CompletableFutureRestAction;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;


public class JsonRequest {
    private JsonRequest() {}

    private static final Gson gson = new Gson();
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient();

    public static @NotNull CompletableFutureRestAction<JsonObject> makeRequest(URL url) {
        return makeRequest(url, null);
    }

    public static @NotNull CompletableFutureRestAction<JsonObject> makeRequest(URL url, @Nullable JsonObject data) {
        CompletableFuture<JsonObject> completableFuture = new CompletableFuture<>();
        Fusion.getConnectionPool().execute(() -> {
            var builder = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json");
            if (data != null) builder.method("POST", RequestBody.create(data.toString(), MEDIA_TYPE));
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
            if (response.isSuccessful()) {
                completableFuture.complete(json);
            } else {
                JsonObject errorObject = json.getAsJsonObject("error");
                if(errorObject != null) {
                    String status = errorObject.get("status").getAsString();
                    String message = errorObject.get("message").getAsString();
                    completableFuture.completeExceptionally(new CommandException(message).addField("Status", status, false));
                } else completableFuture.completeExceptionally(new CommandException("Unknown HTTP error occurred. Code %s".formatted(response.code())));
            }
        });
        return CompletableFutureRestAction.of(completableFuture);
    }
}
