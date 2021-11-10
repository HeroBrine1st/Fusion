package ru.herobrine1st.fusion.command;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.herobrine1st.fusion.Config;
import ru.herobrine1st.fusion.api.command.CommandContext;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class YoutubeCommand extends AbstractSearchCommand {
    private static final HttpUrl URL = HttpUrl.parse("https://www.googleapis.com/youtube/v3/search");
    private static final Logger logger = LoggerFactory.getLogger(YoutubeCommand.class);

    @Override
    protected HttpUrl getUrl(CommandContext ctx) {
        assert URL != null;
        HttpUrl.Builder httpBuilder = URL.newBuilder()
                .addQueryParameter("part", "snippet")
                .addQueryParameter("type", ctx.<String>getArgument("type").orElse("video")) // channel, playlist
                .addQueryParameter("key", Config.getGoogleCustomSearchApiKey())
                .addQueryParameter("maxResults", ctx.<Integer>getArgument("max").orElse(25).toString())
                .addQueryParameter("q", URLEncoder.encode(ctx.<String>getArgument("query").orElseThrow(), StandardCharsets.UTF_8));
        ctx.<String>getArgument("type").ifPresent(it -> httpBuilder.addQueryParameter("fileType", it));
        return httpBuilder.build();
    }

    private static String getUrl(JsonObject json) {
        JsonObject idObject = json.getAsJsonObject("id");
        return switch (idObject.get("kind").getAsString()) {
            case "youtube#video" -> "https://youtube.com/watch?v=" + idObject.get("videoId").getAsString();
            case "youtube#channel" -> "https://youtube.com/channel/" + idObject.get("channelId").getAsString();
            case "youtube#playlist" -> "https://www.youtube.com/playlist?list=" + idObject.get("playlistId").getAsString();
            default -> throw new RuntimeException("Апи дал йобу");
        };
    }

    protected Message getMessage(CommandContext ctx, JsonObject json, int index, int size) {
        return new MessageBuilder()
                .setContent("Video %s/%s for query \"%s\": %s".formatted(index + 1, size,
                        ctx.<String>getArgument("query").orElseThrow(),
                        getUrl(json.getAsJsonArray("items").get(index).getAsJsonObject())))
                .build();
    }
}
