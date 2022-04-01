package ru.herobrine1st.fusion.old.command;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import okhttp3.HttpUrl;
import ru.herobrine1st.fusion.Config;
import ru.herobrine1st.fusion.api.command.CommandContext;

public class YoutubeCommand extends AbstractSearchCommand {
    private static final HttpUrl URL = HttpUrl.parse("https://www.googleapis.com/youtube/v3/search");

    @Override
    protected HttpUrl getUrl(CommandContext ctx) {
        assert URL != null;
        return URL.newBuilder()
                .addQueryParameter("part", "snippet")
                .addQueryParameter("type", ctx.<String>getArgument("type").orElse("video")) // channel, playlist
                .addQueryParameter("key", Config.getYoutubeSearchApiKey())
                .addQueryParameter("maxResults", ctx.<Integer>getArgument("max").orElse(25).toString())
                .addQueryParameter("q", ctx.<String>getArgument("query").orElseThrow())
                .build();
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
