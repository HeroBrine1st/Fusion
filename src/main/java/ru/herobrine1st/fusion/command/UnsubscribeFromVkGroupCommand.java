package ru.herobrine1st.fusion.command;

import com.google.gson.JsonObject;
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
import ru.herobrine1st.fusion.net.JsonRequest;
import ru.herobrine1st.fusion.util.VkApiUtil;

import javax.persistence.Query;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;

public class UnsubscribeFromVkGroupCommand implements CommandExecutor {
    private static final Pattern pattern = Pattern.compile("(?:https?://)?vk\\.com/(?:club(\\d+)|([^/]+))");
    private static final Logger logger = LoggerFactory.getLogger(UnsubscribeFromVkGroupCommand.class);

    @Override
    public void execute(@NotNull CommandContext ctx) throws CommandException {
        ctx.deferReply(true).queue();
        HttpUrl url = ctx.<HttpUrl>getArgument("group").orElseThrow();
        var matcher = pattern.matcher(url.toString());
        if (!matcher.matches()) {
            ctx.getHook().sendMessage("URL is not group").queue();
            return;
        }
        String groupId = Objects.requireNonNullElse(matcher.group(2), matcher.group(1));
        JsonRequest.makeRequest(
                        VkApiUtil.getHttpUrlBuilder("groups.getById")
                                .addQueryParameter("group_id", groupId)
                                .build())
                .thenApply(jsonResponse -> {
                    if (!jsonResponse.response().isSuccessful() || jsonResponse.responseJson().has("error")) {
                        logger.error("Error fetching group information");
                        logger.error("Id: " + groupId);
                        logger.error(jsonResponse.responseJson().toString());
                        throw new CommandException("Error fetching group information");
                    }
                    return jsonResponse.responseJson();
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
                        if(rowsChanged == 0)
                            throw new CommandException("No group with id %s subscribed to this channel".formatted(id));
                    }
                }, Pools.CONNECTION_POOL)
                .thenCompose(o -> ctx.getHook().sendMessage("Successfully unsubscribed").submit())
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
