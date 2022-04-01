package ru.herobrine1st.fusion.old.parser;

import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import ru.herobrine1st.fusion.api.command.CommandContext;
import ru.herobrine1st.fusion.api.command.option.parser.ParserElement;
import ru.herobrine1st.fusion.api.exception.ArgumentParseException;
import ru.herobrine1st.fusion.api.exception.NoSuchArgumentException;

public class URLParserElement extends ParserElement<URLParserElement, HttpUrl> {
    private String host = null;

    public URLParserElement(String name, String description) {
        super(name, description);
    }

    public URLParserElement setHost(String host) {
        this.host = host;
        return this;
    }

    @Override
    public @NotNull OptionData getOptionData() {
        return new OptionData(OptionType.STRING, name, description, required);
    }

    @Override
    public HttpUrl parseSlash(CommandContext ctx, CommandInteraction interaction) throws ArgumentParseException {
        OptionMapping urlOption = interaction.getOption(name);
        if (urlOption == null) throw new NoSuchArgumentException(this);
        String url = urlOption.getAsString();
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) throw new ArgumentParseException("Invalid URL provided");
        if (host != null && !httpUrl.host().equals(host)) {
            throw new ArgumentParseException("Invalid URL host");
        }
        return httpUrl;
    }
}
