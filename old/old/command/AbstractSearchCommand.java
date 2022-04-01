package ru.herobrine1st.fusion.old.command;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import ru.herobrine1st.fusion.api.command.CommandContext;
import ru.herobrine1st.fusion.api.command.CommandExecutor;
import ru.herobrine1st.fusion.api.command.State;
import ru.herobrine1st.fusion.api.exception.CommandException;
import ru.herobrine1st.fusion.api.restaction.CompletableFutureAction;
import ru.herobrine1st.fusion.net.JsonRequest;

import java.util.concurrent.CompletableFuture;

public abstract class AbstractSearchCommand implements CommandExecutor {

    protected abstract HttpUrl getUrl(CommandContext ctx);

    protected abstract Message getMessage(CommandContext ctx, JsonObject json, int index, int count);

    @Override
    public void execute(@NotNull CommandContext ctx) throws CommandException {
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
                        JsonObject errorObject = jsonResponse.json().getAsJsonObject("error");
                        if (errorObject != null) {
                            String status = errorObject.get("status").getAsString();
                            String message = errorObject.get("message").getAsString();
                            if (status.equals("RESOURCE_EXHAUSTED")) {
                                throw new CommandException("Reached API daily limit. Try this command later.");
                            }
                            throw new CommandException("Message: %s, Status: %s".formatted(message, status));
                        } else
                            throw new CommandException("Unknown HTTP error occurred. Code %s".formatted(jsonResponse.response().code()));
                    }
                    if (!jsonResponse.json().has("items"))
                        throw new CommandException("No results");
                    size.setValue(jsonResponse.json().getAsJsonArray("items").size());
                    if (size.getValue() == 0) throw new CommandException("No results");
                    return jsonResponse.json();
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
                        .editOriginal(getMessage(ctx, json, index, size.getValue()))
                        .setActionRows(ActionRow.of(
                                        Button.secondary("first", "<< First").withDisabled(index == 0),
                                        Button.primary("prev", "< Prev").withDisabled(index == 0),
                                        Button.primary("next", "Next >").withDisabled(index == size.getValue() - 1),
                                        Button.secondary("last", "Last >>").withDisabled(index == size.getValue() - 1)),
                                ActionRow.of(
                                        Button.danger("delete", "Delete this message")
                                ))
                )
                .queue(ctx::submitComponents, throwable -> {
                    if (throwable != null) {
                        ctx.handleException(throwable);
                    }
                });
    }
}
