package ru.herobrine1st.fusion.module.googlesearch.command;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.RestAction;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import ru.herobrine1st.fusion.api.command.CommandContext;
import ru.herobrine1st.fusion.api.command.CommandExecutor;
import ru.herobrine1st.fusion.api.exception.CommandException;
import ru.herobrine1st.fusion.module.googlesearch.Config;
import ru.herobrine1st.fusion.module.googlesearch.network.JsonRequest;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Random;

public class YoutubeCommand implements CommandExecutor {
    private static final String URL = "https://www.googleapis.com/youtube/v3/search";
    private final Random random = new Random();

    private URL getUrl(CommandContext ctx) {
        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(URL)).newBuilder()
                .addQueryParameter("part", "snippet")
                .addQueryParameter("type", ctx.<String>getOne("type").orElse("video")) // channel, playlist
                .addQueryParameter("key", Config.getGoogleCustomSearchApiKey())
                .addQueryParameter("maxResults", ctx.<Integer>getOne("max").orElse(25).toString())
                .addQueryParameter("q", URLEncoder.encode(ctx.<String>getOne("query").orElseThrow(), StandardCharsets.UTF_8));
        ctx.<String>getOne("type").ifPresent(it -> httpBuilder.addQueryParameter("fileType", it));
        return httpBuilder.build().url();
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

    private RestAction<?> doCycle(CommandContext ctx, JsonObject json, int index) {
        if (json.getAsJsonArray("items") != null && json.getAsJsonArray("items").size() != 0) {
            int size = json.getAsJsonArray("items").size();
            ActionRow actionRow = ActionRow.of(
                    Button.secondary("first", "<< First").withDisabled(index == 0),
                    Button.primary("prev", "< Prev").withDisabled(index == 0),
                    Button.primary("next", "Next >").withDisabled(index == size - 1),
                    Button.secondary("last", "Last >>").withDisabled(index == size - 1));

            return ctx.reply(new MessageBuilder()
                            .setContent("Video %s/%s for query \"%s\": %s".formatted(index+1, size, ctx.<String>getOne("query").orElseThrow(), getUrl(json.getAsJsonArray("items").get(index).getAsJsonObject())))
                            .setActionRows(actionRow)
                            .build())
                    .flatMap(it -> ctx.getButtonClickEventRestAction())
                    .flatMap(it -> {
                        String componentId = it.getComponentId();
                        return doCycle(ctx, json, switch (componentId) {
                            case "prev" -> index - 1;
                            case "next" -> index + 1;
                            case "first" -> 0;
                            case "last" -> size - 1;
                            default -> throw new RuntimeException("Апи дискорда дал йобу");
                        });
                    });
        } else {
            return ctx.replyError("No results found");
        }
    }
    @Override
    public void execute(@NotNull CommandContext ctx) throws CommandException {
        if (Config.getYoutubeSearchApiKey() == null) {
            throw new CommandException("No API key found");
        }
        ctx.setEditOriginal(true);
        JsonRequest.makeRequest(getUrl(ctx))
                .flatMap(json -> {
                            int size;
                            if (json.getAsJsonArray("items") != null && json.getAsJsonArray("items").size() != 0)
                                size = json.getAsJsonArray("items").size();
                            else size = 0;
                            return doCycle(ctx, json, ctx.<Integer>getOne("index")
                                    .map(integer -> Math.max(Math.min(size - 1, integer), 0))
                                    .orElseGet(() -> random.nextInt(Math.min(Math.max(size , 1), 5))));
                        }
                )
                .queue(null, ctx::replyException);
    }
}
