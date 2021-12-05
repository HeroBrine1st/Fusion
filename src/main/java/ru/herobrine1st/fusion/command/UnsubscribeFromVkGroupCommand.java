package ru.herobrine1st.fusion.command;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import okhttp3.HttpUrl;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.herobrine1st.fusion.Fusion;
import ru.herobrine1st.fusion.Pools;
import ru.herobrine1st.fusion.api.command.CommandContext;
import ru.herobrine1st.fusion.api.command.CommandExecutor;
import ru.herobrine1st.fusion.api.exception.CommandException;
import ru.herobrine1st.fusion.entity.VkGroupSubscriberEntity;
import ru.herobrine1st.fusion.net.JsonRequest;
import ru.herobrine1st.fusion.util.VkApiUtil;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;

public class UnsubscribeFromVkGroupCommand implements CommandExecutor {
    private static final Pattern pattern = Pattern.compile("(?:https?://)?vk\\.com/(?:(?:club|public)(\\d+)|([^/]+))");
    private static final Logger logger = LoggerFactory.getLogger(UnsubscribeFromVkGroupCommand.class);

    private CompletableFuture<Message> removeByLink(@NotNull CommandContext ctx, HttpUrl url) {
        var matcher = pattern.matcher(url.toString());
        if (!matcher.matches()) {
            throw new CommandException("URL is not leading to a group");
        }
        String groupId = Objects.requireNonNullElse(matcher.group(2), matcher.group(1));
        return JsonRequest.makeRequest(
                        VkApiUtil.getHttpUrlBuilder("groups.getById")
                                .addQueryParameter("group_id", groupId)
                                .build())
                .thenApply(jsonResponse -> {
                    if (!jsonResponse.response().isSuccessful() || jsonResponse.json().has("error")) {
                        logger.error("Error fetching group information");
                        logger.error("Id: " + groupId);
                        logger.error(jsonResponse.json().toString());
                        throw new CommandException("Error fetching group information");
                    }
                    return jsonResponse.json();
                })
                .thenAcceptAsync(json -> {
                    JsonObject groupInfo = json.getAsJsonArray("response").get(0).getAsJsonObject();
                    long id = groupInfo.get("id").getAsLong();
                    try (Session session = Fusion.getSessionFactory().openSession()) {
                        Query query = session.createQuery(
                                        "DELETE FROM VkGroupSubscriberEntity entity " +
                                                "WHERE entity.channelId=:channelId AND entity.group.id=:groupId")
                                .setParameter("channelId", Objects.requireNonNull(ctx.getEvent().getChannel()).getIdLong())
                                .setParameter("groupId", id);
                        Transaction transaction = session.beginTransaction();
                        int rowsChanged;
                        try {
                            rowsChanged = query.executeUpdate();
                            transaction.commit();
                        } catch (Exception e) {
                            transaction.rollback();
                            throw new RuntimeException(e);
                        }
                        if (rowsChanged == 0)
                            throw new CommandException("No group with id %s subscribed to this channel".formatted(id));
                    }
                }, Pools.CONNECTION_POOL)
                .thenCompose(o -> ctx.getHook().sendMessage("Successfully unsubscribed").submit());
    }


    private static String ensureStringSize(String s) {
        return s.length() > 100 ? s.substring(0, 100 - 1) + "…" : s;
    }

    private CompletableFuture<Message> removeSelected(CommandContext ctx) {
        return new CompletableFuture<List<VkGroupSubscriberEntity>>().completeAsync(() -> {
                    try (Session session = Fusion.getSessionFactory().openSession()) {
                        TypedQuery<VkGroupSubscriberEntity> query = session.createQuery(
                                        "SELECT entity FROM VkGroupSubscriberEntity entity " +
                                                "WHERE entity.channelId=:channelId " +
                                                "JOIN entity.group", VkGroupSubscriberEntity.class)
                                .setParameter("channelId", Objects.requireNonNull(ctx.getEvent().getChannel()).getIdLong());
                        return query.getResultList();
                    }
                }, Pools.CONNECTION_POOL)
                .thenCompose(list -> {
                    SelectionMenu menu = SelectionMenu.create("groups")
                            .addOptions(list.stream()
                                    .map(it -> SelectOption.of(
                                            ensureStringSize(it.getGroup().getName()),
                                            String.valueOf(it.getId())
                                    ))
                                    .toList()
                            )
                            .setMaxValues(25)
                            .setPlaceholder("Select groups to unsubscribe")
                            .build();
                    return ctx.getHook().sendMessage(new MessageBuilder()
                            .setContent("Select groups to unsubscribe")
                            .setActionRows(ActionRow.of(menu))
                            .build()).submit();
                })
                .thenCompose(ctx::waitForComponentInteraction)
                .thenApply(event -> {
                    if (!(event instanceof SelectionMenuEvent selectionMenuEvent)) throw new RuntimeException();
                    return selectionMenuEvent.getInteraction().getValues().stream().map(Long::parseLong).toList();
                })
                .thenApplyAsync(selectOptions -> {
                    try (Session session = Fusion.getSessionFactory().openSession()) {
                        Query query = session.createQuery("DELETE FROM VkGroupSubscriberEntity entity" +
                                        "WHERE entity.channelId=:channelId AND entity.id IN :selection") // Validated by DBMS
                                .setParameter("channelId", Objects.requireNonNull(ctx.getEvent().getChannel()).getIdLong())
                                .setParameterList("selection", selectOptions);
                        return query.executeUpdate();
                    }
                }, Pools.CONNECTION_POOL)
                .thenCompose(count -> ctx.getHook().editOriginal("Unsubscribed from %d groups.".formatted(count)).submit());
    }

    @Override
    public void execute(@NotNull CommandContext ctx) throws CommandException {
        ctx.deferReply(true).queue();
        Optional<HttpUrl> url = ctx.getArgument("group");
        (url.isPresent() ? removeByLink(ctx, url.get()) : removeSelected(ctx))
                .whenComplete((v, throwable) -> {
                    if (throwable != null) {
                        if (throwable instanceof CompletionException && throwable.getCause() instanceof CommandException)
                            ctx.getHook().sendMessage(throwable.getMessage()).queue();
                        else
                            ctx.getHook().sendMessage("Unknown exception occurred").queue();
                        logger.error("Error executing SubscribeToVkGroupCommand", throwable);
                    }
                });
    }
}
