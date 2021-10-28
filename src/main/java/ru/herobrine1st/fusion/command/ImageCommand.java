package ru.herobrine1st.fusion.command;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.RestAction;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.herobrine1st.fusion.api.command.CommandContext;
import ru.herobrine1st.fusion.api.command.CommandExecutor;
import ru.herobrine1st.fusion.api.command.State;
import ru.herobrine1st.fusion.api.exception.CommandException;
import ru.herobrine1st.fusion.Config;
import ru.herobrine1st.fusion.api.restaction.CompletableFutureAction;
import ru.herobrine1st.fusion.network.JsonRequest;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class ImageCommand implements CommandExecutor {
    private final static String URL = "https://www.googleapis.com/customsearch/v1";
    private final static Logger logger = LoggerFactory.getLogger(ImageCommand.class);

    private URL getUrl(CommandContext ctx) {
        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(URL)).newBuilder()
                .addQueryParameter("num", "10")
                .addQueryParameter("start", "1")
                .addQueryParameter("searchType", "image")
                .addQueryParameter("cx", Config.getGoogleCustomSearchEngineId())
                .addQueryParameter("key", Config.getGoogleCustomSearchApiKey())
                .addQueryParameter("safe", "1")
                .addQueryParameter("q", URLEncoder.encode(ctx.<String>getArgument("query").orElseThrow(), StandardCharsets.UTF_8));
        ctx.<String>getArgument("type").ifPresent(it -> httpBuilder.addQueryParameter("fileType", it));
        return httpBuilder.build().url();
    }

    private MessageEmbed getEmbedFromJson(CommandContext ctx, JsonObject json, int index, int count) {
        JsonObject image = json.getAsJsonArray("items").get(index).getAsJsonObject();
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(
                        image.get("title").getAsString(),
                        image.getAsJsonObject("image").get("contextLink").getAsString())
                .setImage(image.get("link").getAsString());
        if (image.get("mime").getAsString().equals("image/svg+xml"))
            builder.setDescription("SVG images may not display on some clients.");
        return builder.setFooter("Image %s/%s\nQuery: \"%s\"".formatted(index + 1, count, ctx.<String>getArgument("query").orElseThrow()))
                .build();
    }

    @Override
    public void execute(@NotNull CommandContext ctx) throws CommandException {
        if (Config.getGoogleCustomSearchApiKey() == null || Config.getGoogleCustomSearchEngineId() == null) {
            throw new CommandException("No API key found");
        }
        ctx.deferReply().queue();
        State<Integer> size = ctx.useState(null);
        CompletableFuture<JsonObject> requestFuture = ctx.useEffect(() -> JsonRequest.makeRequest(getUrl(ctx))
                .thenApply(json -> {
                    size.setValue(json.getAsJsonArray("items").size());
                    return json;
                }));
        int index = ctx.useComponent(0, (id, old) -> switch (id) {
            case "prev" -> old - 1;
            case "next" -> old + 1;
            case "first" -> 0;
            case "last" -> size.getValue() - 1;
            default -> throw new RuntimeException();
        }, "prev", "next", "first", "last");
        CompletableFutureAction.of(requestFuture)
                .flatMap(json -> ctx.getHook()
                        .editOriginalEmbeds(getEmbedFromJson(ctx, json, index, size.getValue()))
                        .setActionRow(Button.secondary("first", "<< First").withDisabled(index == 0),
                                Button.primary("prev", "< Prev").withDisabled(index == 0),
                                Button.primary("next", "Next >").withDisabled(index == size.getValue() - 1),
                                Button.secondary("last", "Last >>").withDisabled(index == size.getValue() - 1)))
                .queue(ctx::submitComponents, throwable -> {
                    if(throwable instanceof CommandException commandException) {
                        ctx.getHook().sendMessage(commandException.getMessage()).queue();
                    } else {
                        ctx.getHook().sendMessage("Unhandled error occurred").queue();
                    }
                    logger.error("Error executing img command", throwable);
                });
    }
}
