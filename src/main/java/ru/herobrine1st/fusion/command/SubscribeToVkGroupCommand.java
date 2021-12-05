package ru.herobrine1st.fusion.command;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
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
import ru.herobrine1st.fusion.entity.VkGroupEntity;
import ru.herobrine1st.fusion.entity.VkGroupSubscriberEntity;
import ru.herobrine1st.fusion.net.JsonRequest;
import ru.herobrine1st.fusion.util.VkApiUtil;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;

public class SubscribeToVkGroupCommand implements CommandExecutor {
    private static final Pattern pattern = Pattern.compile("(?:https?://)?vk\\.com/(?:(?:club|public)(\\d+)|([^/]+))");
    private static final Logger logger = LoggerFactory.getLogger(SubscribeToVkGroupCommand.class);

    @Override
    public void execute(@NotNull CommandContext ctx) throws CommandException {
        ctx.deferReply(true).queue();
        HttpUrl url = ctx.<HttpUrl>getArgument("group").orElseThrow();
        var matcher = pattern.matcher(url.toString());
        if (!matcher.matches()) {
            ctx.getHook().sendMessage("URL is not leading to a group").queue();
            return;
        }
        String groupId = Objects.requireNonNullElse(matcher.group(2), matcher.group(1));
        new CompletableFuture<Void>()
                .completeAsync(() -> {
                    try (Session session = Fusion.getSessionFactory().openSession()) {
                        TypedQuery<Long> query = session.createQuery(
                                        "SELECT count(entity) FROM VkGroupSubscriberEntity entity " +
                                                "WHERE entity.channelId=:channelId", Long.class)
                                .setParameter("channelId", Objects.requireNonNull(ctx.getEvent().getChannel()).getIdLong());
                        long count = query.getSingleResult();
                        if (count >= 25)
                            throw new CommandException("This channel has reached 25 subscriptions limit");
                    }
                    return null;
                }, Pools.CONNECTION_POOL)
                .thenCompose(unused -> JsonRequest.makeRequest(
                        VkApiUtil.getHttpUrlBuilder("groups.getById")
                                .addQueryParameter("group_id", groupId)
                                .build()))
                .thenApply(jsonResponse -> {
                    if (!jsonResponse.response().isSuccessful() || jsonResponse.json().has("error")) {
                        logger.error("Error fetching group information");
                        logger.error("Id: " + groupId);
                        logger.error(jsonResponse.json().toString());
                        throw new CommandException("Error fetching group information");
                    }
                    return jsonResponse.json();
                })
                .thenApplyAsync(json -> { // Check if already subscribed
                    JsonObject groupInfo = json.getAsJsonArray("response").get(0).getAsJsonObject();
                    long id = groupInfo.get("id").getAsLong();
                    try (Session session = Fusion.getSessionFactory().openSession()) {
                        TypedQuery<Long> query = session.createQuery(
                                        "SELECT count(entity) FROM VkGroupSubscriberEntity entity " +
                                                "WHERE entity.channelId=:channelId AND entity.group.id=:groupId", Long.class)
                                .setParameter("channelId", Objects.requireNonNull(ctx.getEvent().getChannel()).getIdLong())
                                .setParameter("groupId", id);
                        long count = query.getSingleResult();
                        if (count > 0)
                            throw new CommandException("This channel is already subscribed to this group");
                        return groupInfo;
                    }
                }, Pools.CONNECTION_POOL)
                .thenApplyAsync(groupInfo -> {
                    long id = groupInfo.get("id").getAsLong();
                    VkGroupEntity entity;
                    try (Session session = Fusion.getSessionFactory().openSession()) {
                        TypedQuery<VkGroupEntity> query = session.createQuery(
                                        "SELECT entity FROM VkGroupEntity entity " +
                                                "WHERE entity.id=:id", VkGroupEntity.class)
                                .setParameter("id", id);
                        try {
                            entity = query.getSingleResult();
                        } catch (NoResultException exception) {
                            entity = new VkGroupEntity();
                            entity.setId(id);
                            entity.setLastWallPostId(-1);
                        }
                    }
                    entity.setName(groupInfo.get("name").getAsString());
                    entity.setAvatarUrl(groupInfo.get("photo_200").getAsString());
                    return entity;
                }, Pools.CONNECTION_POOL)
                .thenCompose(entity -> {
                    if (entity.getLastWallPostId() == -1) {
                        return JsonRequest.makeRequest(VkApiUtil.getHttpUrlBuilder("wall.get")
                                        .setQueryParameter("owner_id", String.valueOf(-entity.getId()))
                                        .build())
                                .thenApply(jsonResponse -> {
                                    if (!jsonResponse.response().isSuccessful() || jsonResponse.json().has("error")) {
                                        logger.error("Error fetching group information");
                                        logger.error("Id: " + groupId);
                                        logger.error(jsonResponse.json().toString());
                                        throw new CommandException("Error fetching group wall");
                                    }
                                    return jsonResponse.json();
                                }).thenApply(json -> {
                                    entity.setLastWallPostId(json
                                            .getAsJsonObject("response")
                                            .getAsJsonArray("items")
                                            .get(0).getAsJsonObject()// FIXME filter is_pinned
                                            .get("id").getAsLong());
                                    return entity;
                                });
                    }
                    return CompletableFuture.completedFuture(entity);
                })
                .thenApplyAsync(entity -> {
                    try (Session session = Fusion.getSessionFactory().openSession()) {
                        Transaction transaction = session.beginTransaction();
                        try {
                            session.saveOrUpdate(entity);
                            transaction.commit();
                        } catch (Exception e) {
                            transaction.rollback();
                            throw new RuntimeException(e);
                        }
                    }
                    return entity;
                }, Pools.CONNECTION_POOL)
                .thenApplyAsync(entity -> {
                    VkGroupSubscriberEntity vkGroupSubscriber = new VkGroupSubscriberEntity();
                    vkGroupSubscriber.setGroup(entity);
                    vkGroupSubscriber.setChannelId(Objects.requireNonNull(ctx.getEvent().getChannel()).getIdLong());
                    vkGroupSubscriber.setGuildId(Objects.requireNonNull(ctx.getEvent().getGuild()).getIdLong());
                    try (Session session = Fusion.getSessionFactory().openSession()) {
                        Transaction transaction = session.beginTransaction();
                        try {
                            session.save(vkGroupSubscriber);
                            transaction.commit();
                        } catch (Exception e) {
                            transaction.rollback();
                            throw new RuntimeException(e);
                        }
                    }
                    return entity;
                }, Pools.CONNECTION_POOL)
                .thenAccept(entity -> ctx.getHook().sendMessage(new MessageBuilder()
                        .setEmbeds(new EmbedBuilder()
                                .setColor(0x00FF00)
                                .setAuthor(entity.getName(), "https://vk.com/club" + entity.getId(), entity.getAvatarUrl())
                                .setDescription("Complete. This embed is example post.")
                                .build())
                        .build()).queue())
                .whenComplete((v, throwable) -> {
                    if (throwable != null) {
                        ctx.handleException(throwable);
                    }
                });
    }
}
