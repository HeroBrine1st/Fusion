package ru.herobrine1st.fusion.module.redditdownloader;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.herobrine1st.fusion.api.Fusion;
import ru.herobrine1st.fusion.api.annotation.FusionModule;
import ru.herobrine1st.fusion.api.command.args.GenericArguments;
import ru.herobrine1st.fusion.api.command.build.FusionCommand;
import ru.herobrine1st.fusion.api.event.FusionInitializationEvent;
import ru.herobrine1st.fusion.api.exception.CommandException;
import ru.herobrine1st.fusion.module.redditdownloader.command.RedditDownloadCommand;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@FusionModule(id = "redditdownloader", name = "Reddit Downloader Module")
public class RedditDownloaderModule {
    private static final Logger logger = LoggerFactory.getLogger(RedditDownloaderModule.class);
    private static final URL CSRFUrl;
    private static final URL requestUrl;
    private static final Pattern inputFieldRegex = Pattern.compile("<input.+name=\"([^\"]+)\".+value=\"([^\"]+)\".+/>");
    private static final OkHttpClient client = new OkHttpClient().newBuilder()
            .cookieJar(new CookieJar() {
                private final List<Cookie> cookies = new ArrayList<>();
                @Override
                synchronized public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
                    cookies.addAll(list);
                }

                @NotNull
                @Override
                public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
                    return cookies;
                }
            })
            .build();
    private static final Gson gson = new Gson();

    static {  // Какого хуя MalformedURLException не наследует RuntimeException ???
        try { // А тем более нахуя ему наследовать IOException, это же не сетевая ошибка
            CSRFUrl = new URL("https://www.reddit.tube/");
            requestUrl = new URL("https://www.reddit.tube/services/get_video");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public void onInit(FusionInitializationEvent event) {
        Fusion.getCommandManager().registerCommand(FusionCommand.withArguments("reddit", "Download reddit video")
                .addOptions(GenericArguments.string("url", "Link to a post"))
                .setExecutor(new RedditDownloadCommand())
                .setShortName("Reddit Downloader")
        );
    }

    public static CompletableFuture<JsonObject> getJson(String url) {
        CompletableFuture<JsonObject> completableFuture = new CompletableFuture<>();
        Fusion.getConnectionPool().execute(() -> {
            Request csrfRequest = new Request.Builder()
                    .url(CSRFUrl)
                    .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) Gecko/20100101 Firefox/91.0")
                    .build();
            var fields = new HashMap<String, String>();
            Response it;
            try {
                it = client.newCall(csrfRequest).execute();
            } catch (IOException e) {
                completableFuture.completeExceptionally(e);
                return;
            }
            Matcher matcher;
            try {
                matcher = inputFieldRegex.matcher(Objects.requireNonNull(it.body()).string());
            } catch (IOException e) {
                completableFuture.completeExceptionally(e);
                return;
            }
            while (matcher.find()) {
                var key = matcher.group(1);
                var value = matcher.group(2);
                fields.putIfAbsent(key, value);
            }
            fields.put("url", url);
            fields.put("zip", "");
            var requestBody = new FormBody.Builder();
            fields.forEach(requestBody::add);
            var request = new Request.Builder()
                    .url(requestUrl)
                    .post(requestBody.build())
                    .header(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) Gecko/20100101 Firefox/91.0");
            String result;
            try {
                result = Objects.requireNonNull(client.newCall(request.build()).execute().body()).string();
            } catch (IOException e) {
                completableFuture.completeExceptionally(e);
                return;
            }
            var json = gson.fromJson(result, JsonObject.class);
            if (!"ok".equals(json.get("status").getAsString())) {
                if ("error".equals(json.get("status").getAsString()) && json.get("msg") != null) {
                    completableFuture.completeExceptionally(new CommandException(json.get("msg").getAsString()));
                } else {
                    logger.error(json.toString());
                    completableFuture.completeExceptionally(new CommandException("Unknown API error occurred."));
                }
            }
            completableFuture.complete(json);
        });
        return completableFuture;
    }
}
