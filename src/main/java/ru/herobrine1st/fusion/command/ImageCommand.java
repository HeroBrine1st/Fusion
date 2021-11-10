package ru.herobrine1st.fusion.command;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import okhttp3.HttpUrl;
import ru.herobrine1st.fusion.Config;
import ru.herobrine1st.fusion.api.command.CommandContext;

public class ImageCommand extends AbstractSearchCommand {
    private final static HttpUrl URL = HttpUrl.parse("https://www.googleapis.com/customsearch/v1");

    @Override
    protected HttpUrl getUrl(CommandContext ctx) {
        assert URL != null;
        HttpUrl.Builder httpBuilder = URL.newBuilder()
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

    @Override
    protected Message getMessage(CommandContext ctx, JsonObject json, int index, int count) {
        JsonObject image = json.getAsJsonArray("items").get(index).getAsJsonObject();
        return new MessageBuilder()
                .setEmbeds(new EmbedBuilder()
                        .setTitle(image.get("title").getAsString(),
                                image.getAsJsonObject("image").get("contextLink").getAsString())
                        .setImage(image.get("link").getAsString())
                        .setDescription(image.get("mime").getAsString().equals("image/svg+xml")
                                ? "SVG images may not display on some clients." : null)
                        .setFooter("Image %s/%s".formatted(index + 1, count))
                        .build())
                .build();
    }
}
