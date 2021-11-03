package ru.herobrine1st.fusion.tasks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.herobrine1st.fusion.Fusion;
import ru.herobrine1st.fusion.entity.VkGroupEntity;
import ru.herobrine1st.fusion.entity.VkGroupSubscriberEntity;
import ru.herobrine1st.fusion.net.JsonRequest;
import ru.herobrine1st.fusion.util.VkApiUtil;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VkGroupFetchTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(VkGroupFetchTask.class);

    private String getBiggestPhotoURL(JsonObject attachment) {
        List<JsonObject> sizes = jsonArrayToList(attachment.getAsJsonObject("photo").getAsJsonArray("sizes"));
        return sizes.stream().max(Comparator.comparingInt(a -> a.get("width").getAsInt())).orElseThrow().get("url").getAsString();
    }

    private List<JsonObject> jsonArrayToList(JsonArray array) {
        List<JsonObject> arrayList = new ArrayList<>();
        for (JsonElement it : array) {
            if (!(it instanceof JsonObject jsonObject))
                throw new RuntimeException("Array contains not only JsonObjects");
            arrayList.add(jsonObject);
        }
        return arrayList;
    }

    // Костыльный функци-АНААААААААААААААААААААААЛ
    @Override
    public void run() {
        try (Session session = Fusion.getSessionFactory().openSession()) {
            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<VkGroupEntity> criteriaQuery = criteriaBuilder.createQuery(VkGroupEntity.class);
            Root<VkGroupEntity> selection = criteriaQuery.from(VkGroupEntity.class);
            criteriaQuery.select(selection)
                    .where(criteriaBuilder.isNotEmpty(selection.get("subscribers")));

            List<VkGroupEntity> groups = session.createQuery(criteriaQuery).getResultList();
            for (VkGroupEntity group : groups) {
                JsonRequest.JsonResponse result = JsonRequest.makeRequest(
                        VkApiUtil.getHttpUrlBuilder("wall.get")
                                .addQueryParameter("owner_id", String.valueOf(-group.getId()))
                                .build()
                ).join();
                if (!result.response().isSuccessful() || result.responseJson().has("error")) {
                    logger.error("Exception fetching vk wall");
                    logger.error(result.response().toString());
                    continue;
                }
                List<JsonObject> wall = jsonArrayToList(result.responseJson().getAsJsonObject("response").getAsJsonArray("items"))
                        .stream()
                        .filter(it -> it.get("id").getAsLong() > group.getLastWallPostId())
                        .sorted(Comparator.comparing((it) -> it.get("id").getAsLong()))
                        .toList();
                for (JsonObject post : wall) {
                    long id = post.get("id").getAsLong();
                    group.setLastWallPostId(id);
                    String url = "https://vk.com/club%s?w=wall-%s_%s".formatted(group.getId(), group.getId(), id);
                    MessageEmbed.AuthorInfo fuckingAuthorInfo = new MessageEmbed.AuthorInfo(group.getName(), url, group.getAvatarUrl(), null);
                    MessageEmbed.ImageInfo fuckingImageInfo = null;
                    StringBuilder footerBuilder = new StringBuilder();
                    List<MessageEmbed> embeds = new ArrayList<>();
                    OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(Instant.ofEpochSecond(post.get("date").getAsLong()), ZoneOffset.UTC);
                    if (post.has("copy_history") && post.getAsJsonArray("copy_history").size() > 0) {
                        post = post.getAsJsonArray("copy_history").get(0).getAsJsonObject();
                        footerBuilder.append("This post is a repost\n");
                    }
                    String text = post.get("text").getAsString();
                    if (text.length() > 2048) {
                        String additionalText = "... Post is too big (%s/2048 symbols)".formatted(text.length());
                        text = text.substring(0, 2048 - additionalText.length()) + additionalText;
                    }

                    if (post.has("attachments") && post.getAsJsonArray("attachments").size() > 0) {
                        List<JsonObject> attachments = jsonArrayToList(post.getAsJsonArray("attachments"));
                        if (!attachments.stream()
                                .allMatch(it -> it.get("type").getAsString().equals("link")
                                        || it.get("type").getAsString().equals("photo"))) {
                            footerBuilder.append("Post contains incompatible attachments\n");
                        }
                        List<JsonObject> attachmentsPhoto = attachments.stream().filter(it -> "photo".equals(it.get("type").getAsString())).toList();
                        if (attachmentsPhoto.size() > 0) {
                            if (attachmentsPhoto.size() > 4)
                                footerBuilder.append("Post contains more than 4 images\n");
                            fuckingImageInfo = new MessageEmbed.ImageInfo(getBiggestPhotoURL(attachmentsPhoto.get(0)), null, 0, 0);
                            attachmentsPhoto.stream().skip(1).limit(3).forEach(it -> {
                                // Because of this fucking resetting url builder when title is null
                                embeds.add(new MessageEmbed(url, null, null, EmbedType.RICH,
                                        null, Role.DEFAULT_COLOR_RAW, null, null,
                                        null, null, null,
                                        new MessageEmbed.ImageInfo(getBiggestPhotoURL(it), null, 0, 0),
                                        null));
                            });
                        }
                        List<JsonObject> attachmentsLink = attachments.stream().filter(it -> "link".equals(it.get("type").getAsString())).toList();
                        if (attachmentsLink.size() > 0) {
                            for (JsonObject attachmentLink : attachmentsLink) {
                                attachmentLink = attachmentLink.getAsJsonObject("link");
                                EmbedBuilder linkEmbedBuilder = new EmbedBuilder()
                                        .setTitle(attachmentLink.get("title").getAsString(), attachmentLink.get("url").getAsString());
                                if (attachmentLink.has("description") && !attachmentLink.get("description").getAsString().isBlank())
                                    linkEmbedBuilder.setDescription(attachmentLink.get("description").getAsString());
                                if (attachmentLink.has("caption") && !attachmentLink.get("caption").getAsString().isBlank())
                                    linkEmbedBuilder.setFooter(attachmentLink.get("caption").getAsString());
                                if (attachmentLink.has("photo") && attachmentLink.get("photo") != null)
                                    linkEmbedBuilder.setImage(getBiggestPhotoURL(attachmentLink));
                                embeds.add(linkEmbedBuilder.build());
                            }
                        }
                    }
                    embeds.add(0, new MessageEmbed(url, null, text, EmbedType.RICH, offsetDateTime,
                            Role.DEFAULT_COLOR_RAW, null, null, fuckingAuthorInfo, null,
                            new MessageEmbed.Footer(footerBuilder.toString(), null, null), fuckingImageInfo, null));
                    for (VkGroupSubscriberEntity it : group.getSubscribers()) {
                        Guild guild = Fusion.getJda().getGuildById(it.getGuildId());
                        TextChannel channel;
                        if (guild != null
                                && (channel = guild.getTextChannelById(it.getChannelId())) != null
                                && guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_WRITE)) {
                            channel.sendMessage(new MessageBuilder()
                                            .setEmbeds(embeds)
                                            .build())
                                    .queue(null, Throwable::printStackTrace);
                        } else {
                            logger.info("Cannot send message to channel %s in guild %s; Removing from subscriptions"
                                    .formatted(it.getChannelId(), it.getGuildId()));
                            Transaction transaction = session.beginTransaction();
                            try {
                                session.delete(it); // Cannot delete directly from group in a cycle
                                transaction.commit();
                            } catch (Exception e) {
                                e.printStackTrace();
                                transaction.rollback();
                            }
                        }
                    }
                }
                Transaction transaction = session.beginTransaction();
                try {
                    session.update(group);
                    transaction.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                    transaction.rollback();
                }
            }
        } catch (Exception e) {
            logger.error("Exception fetching vk group wall", e);
        }
    }
}
