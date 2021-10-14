package ru.herobrine1st.fusion.module.googlesearch.command;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.MessageEmbed;
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

public class ImageCommand implements CommandExecutor {
    private final Random random = new Random();
    private final static String URL = "https://www.googleapis.com/customsearch/v1";

    private URL getUrl(CommandContext ctx) {
        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(URL)).newBuilder()
                .addQueryParameter("num", "10")
                .addQueryParameter("start", "1")
                .addQueryParameter("searchType", "image")
                .addQueryParameter("cx", Config.getGoogleCustomSearchEngineId())
                .addQueryParameter("key", Config.getGoogleCustomSearchApiKey())
                .addQueryParameter("safe", "1")
                .addQueryParameter("q", URLEncoder.encode(ctx.<String>getOne("query").orElseThrow(), StandardCharsets.UTF_8));
        ctx.<String>getOne("type").ifPresent(it -> httpBuilder.addQueryParameter("fileType", it));
        return httpBuilder.build().url();
    }

    private MessageEmbed getEmbedFromJson(CommandContext ctx, JsonObject image, int index, int count) {
        var builder = ctx.getEmbedBase()
                .setTitle(
                        image.get("title").getAsString(),
                        image.getAsJsonObject("image").get("contextLink").getAsString())
                .setImage(image.get("link").getAsString());
        if (image.get("mime").getAsString().equals("image/svg+xml"))
            builder.setDescription("SVG images may not display on some clients.");
        return builder.setFooter(ctx.getFooter("Image %s/%s\nQuery: \"%s\"".formatted(index + 1, count, ctx.<String>getOne("query").orElseThrow())))
                .build();
    }

    private RestAction<?> doCycle(CommandContext ctx, JsonObject json, int index) {
        if (json.getAsJsonArray("items") != null && json.getAsJsonArray("items").size() != 0) {
            int size = json.getAsJsonArray("items").size();
            ActionRow actionRow = ActionRow.of(
                    Button.secondary("first", "<< First").withDisabled(index == 0),
                    Button.primary("prev", "< Prev").withDisabled(index == 0),
                    Button.primary("next", "Next >").withDisabled(index == size - 1),
                    Button.secondary("last", "Last >>").withDisabled(index == size - 1));
            return ctx.replyThenWaitUserClick(getEmbedFromJson(ctx,
                            json.getAsJsonArray("items").get(index).getAsJsonObject(), index, size), actionRow)
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
        if (Config.getGoogleCustomSearchApiKey() == null || Config.getGoogleCustomSearchEngineId() == null) {
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
