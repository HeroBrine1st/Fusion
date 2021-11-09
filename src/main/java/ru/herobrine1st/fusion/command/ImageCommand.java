package ru.herobrine1st.fusion.command;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ImageCommand implements CommandExecutor {
    private final static String URL = "https://www.googleapis.com/customsearch/v1";
    private final static Logger logger = LoggerFactory.getLogger(ImageCommand.class);

    private HttpUrl getUrl(CommandContext ctx) {
        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(URL)).newBuilder()
                .addQueryParameter("num", "10")
                .addQueryParameter("start", "1")
                .addQueryParameter("searchType", "image")
                .addQueryParameter("cx", Config.getGoogleCustomSearchEngineId())
                .addQueryParameter("key", Config.getGoogleCustomSearchApiKey())
                .addQueryParameter("safe", ctx.<String>getArgument("safe").orElse("active"))
                .addQueryParameter("q", ctx.<String>getArgument("query").orElseThrow());
        ctx.<String>getArgument("type").ifPresent(it -> httpBuilder.addQueryParameter("fileType", it));
        return httpBuilder.build();
    }

    private MessageEmbed getEmbedFromJson(JsonObject json, int index, int count) {
        JsonObject image = json.getAsJsonArray("items").get(index).getAsJsonObject();
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(
                        image.get("title").getAsString(),
                        image.getAsJsonObject("image").get("contextLink").getAsString())
                .setImage(image.get("link").getAsString());
        if (image.get("mime").getAsString().equals("image/svg+xml"))
            builder.setDescription("SVG images may not display on some clients.");
        return builder.setFooter("Image %s/%s".formatted(index + 1, count))
                .build();
    }

    @Override
    public void execute(@NotNull CommandContext ctx) throws CommandException {
        if (Config.getGoogleCustomSearchApiKey() == null || Config.getGoogleCustomSearchEngineId() == null) {
            throw new CommandException("No API key found");
        }
        ctx.deferReply(
                Config.getImgCommandEphemeralList().contains(ctx.getUser().getId())
        ).queue();

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
                    if(size.getValue() == 0) throw new CommandException("No results");
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
                                .setEmbeds(getEmbedFromJson(json, index, size.getValue()))
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
