package ru.herobrine1st.fusion.module.redditdownloader.command;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.AttachmentOption;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.herobrine1st.fusion.api.Fusion;
import ru.herobrine1st.fusion.api.command.CommandContext;
import ru.herobrine1st.fusion.api.command.CommandExecutor;
import ru.herobrine1st.fusion.api.exception.CommandException;
import ru.herobrine1st.fusion.module.redditdownloader.RedditDownloaderModule;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RedditDownloadCommand implements CommandExecutor {
    private static final Logger logger = LoggerFactory.getLogger(RedditDownloadCommand.class);

    @Override
    public void execute(@NotNull CommandContext ctx) throws CommandException {
        String url = ctx.<String>getOne("url").orElseThrow();
        JsonObject json;
        byte[] file;
        try {
            json = RedditDownloaderModule.getJson(url).get();
            file = downloadFile(json.get("url").getAsString()).get();
        } catch (InterruptedException e) {
            throw new CommandException("Could not download video: download interrupted", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CommandException commandException) throw commandException;
            else throw new CommandException("Could not download video", e.getCause());
        }
        logger.trace("Downloaded video from " + json.get("url").getAsString());
        logger.trace("Creating embed");
        var embed = ctx.getEmbedBase()
                .setFooter(ctx.getFooter("Using reddit.tube"));
        boolean nsfw;
        if (json.has("video_data")) {
            var videoData = json.getAsJsonObject("video_data");
            embed.setAuthor("u/%s in r/%s".formatted(videoData.get("author").getAsString(), videoData.get("subreddit").getAsString()));
            embed.setTitle(videoData.get("title").getAsString(), videoData.get("url").getAsString());
            nsfw = videoData.get("nsfw").getAsBoolean();
        } else {
            embed.setTitle("Noncritical error occurred");
            embed.setDescription("Post data unavailable, NSFW status unknown - assuming as NSFW content.");
            embed.setColor(ctx.getWarningColor());
            nsfw = true;
        }
        logger.trace("Sending message");
        var message = new MessageBuilder().setEmbed(embed.build()).build();
        var strings = json.get("url").getAsString().split("/");
        var filename = strings[strings.length - 1];
        if (ctx.getEvent() instanceof SlashCommandEvent slashCommandEvent) {
            slashCommandEvent.getHook().sendMessage(message).addFile(file, filename,
                    nsfw ? new AttachmentOption[]{AttachmentOption.SPOILER} : new AttachmentOption[0]).queue();
        } else if (ctx.getEvent() instanceof MessageReceivedEvent messageReceivedEvent) {
            messageReceivedEvent.getMessage().getChannel().sendMessage(message).addFile(file, filename,
                    nsfw ? new AttachmentOption[]{AttachmentOption.SPOILER} : new AttachmentOption[0]).queue();
        } else {
            throw new RuntimeException();
        }
    }

    private static CompletableFuture<byte[]> downloadFile(String url) {
        CompletableFuture<byte[]> completableFuture = new CompletableFuture<>();
        Fusion.getConnectionPool().execute(() -> {
            HttpURLConnection connection;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.connect();
            } catch (IOException e) {
                completableFuture.completeExceptionally(new CommandException("Could not download video", e));
                return;
            }
            if (connection.getContentLength() > Message.MAX_FILE_SIZE) {
                completableFuture.completeExceptionally(new CommandException("File size exceed 8 megabytes: $url"));
            } else {
                try {
                    completableFuture.complete(connection.getInputStream().readAllBytes());
                } catch (IOException e) {
                    completableFuture.completeExceptionally(new CommandException("Could not download video", e));
                }
            }
        });
        return completableFuture;
    }
}
