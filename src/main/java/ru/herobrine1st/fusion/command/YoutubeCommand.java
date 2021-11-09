package ru.herobrine1st.fusion.command;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.herobrine1st.fusion.Config;
import ru.herobrine1st.fusion.api.command.CommandContext;
import ru.herobrine1st.fusion.api.command.CommandExecutor;
import ru.herobrine1st.fusion.api.command.State;
import ru.herobrine1st.fusion.api.exception.CommandException;
import ru.herobrine1st.fusion.api.restaction.CompletableFutureAction;
import ru.herobrine1st.fusion.net.JsonRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class YoutubeCommand implements CommandExecutor {
    private static final String URL = "https://www.googleapis.com/youtube/v3/search";
    private static final Logger logger = LoggerFactory.getLogger(YoutubeCommand.class);

    private HttpUrl getUrl(CommandContext ctx) {
        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(URL)).newBuilder()
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

    @Override
    public void execute(@NotNull CommandContext ctx) throws CommandException {
        if (Config.getYoutubeSearchApiKey() == null) {
            throw new CommandException("No API key found");
        }
        ctx.deferReply().queue();

        // Delete button
        if (ctx.getEvent() instanceof ButtonClickEvent buttonClickEvent && buttonClickEvent.getComponentId().equals("delete")) {
            ctx.getHook().deleteOriginal().queue();
            return;
        }

        State<Integer> size = ctx.useState(null);
        CompletableFuture<JsonObject> requestFuture = ctx.useEffect(() -> JsonRequest.makeRequest(getUrl(ctx))
                .thenApply(jsonResponse -> {
                    if (!jsonResponse.response().isSuccessful()) {
                        JsonObject errorObject = jsonResponse.responseJson().getAsJsonObject("error");
                        if (errorObject != null) {
                            String status = errorObject.get("status").getAsString();
                            String message = errorObject.get("message").getAsString();
                            if (status.equals("RESOURCE_EXHAUSTED")) {
                                throw new CommandException("Reached API daily limit. Try call this command later.");
                            }
                            throw new CommandException("Message: %s, Status: %s".formatted(message, status));
                        } else
                            throw new CommandException("Unknown HTTP error occurred. Code %s".formatted(jsonResponse.response().code()));
                    }
                    if (!jsonResponse.responseJson().has("items"))
                        throw new CommandException("No results");
                    size.setValue(jsonResponse.responseJson().getAsJsonArray("items").size());
                    if (size.getValue() == 0) throw new CommandException("No results");
                    return jsonResponse.responseJson();
                }));
        int index = ctx.useComponent(0, (id, old) -> switch (id) {
            case "prev" -> old - 1;
            case "next" -> old + 1;
            case "first" -> 0;
            case "last" -> size.getValue() - 1;
            default -> old;
        }, "prev", "next", "first", "last");
        CompletableFutureAction.of(requestFuture)
                .flatMap(json -> ctx.getHook()
                        .editOriginal(new MessageBuilder()
                                .setContent("Video %s/%s for query \"%s\": %s".formatted(index + 1, size.getValue(),
                                        ctx.<String>getArgument("query").orElseThrow(),
                                        getUrl(json.getAsJsonArray("items").get(index).getAsJsonObject())))
                                .setActionRows(ActionRow.of(
                                                Button.secondary("first", "<< First").withDisabled(index == 0),
                                                Button.primary("prev", "< Prev").withDisabled(index == 0),
                                                Button.primary("next", "Next >").withDisabled(index == size.getValue() - 1),
                                                Button.secondary("last", "Last >>").withDisabled(index == size.getValue() - 1)),
                                        ActionRow.of(
                                                Button.danger("delete", "Delete this message")
                                        ))
                                .build()
                        ))
                .queue(ctx::submitComponents, throwable -> {
                    if (throwable instanceof CompletionException && throwable.getCause() instanceof CommandException commandException)
                        ctx.getHook().sendMessage(commandException.getMessage()).queue();
                    else ctx.getHook().sendMessage("Unhandled error occurred").queue();
                    logger.error("Error executing img command", throwable);
                });
    }
}
