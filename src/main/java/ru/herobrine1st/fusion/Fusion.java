package ru.herobrine1st.fusion;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.herobrine1st.fusion.api.command.CommandContext;
import ru.herobrine1st.fusion.api.command.GenericArguments;
import ru.herobrine1st.fusion.api.command.option.FusionCommand;
import ru.herobrine1st.fusion.api.manager.CommandManager;
import ru.herobrine1st.fusion.command.ImageCommand;
import ru.herobrine1st.fusion.command.YoutubeCommand;

import javax.security.auth.login.LoginException;

public class Fusion {
    private static final Logger logger = LoggerFactory.getLogger("Fusion");

    public static void main(String[] args) {
        JDA jda;
        try {
            jda = JDABuilder.createLight(Config.getDiscordToken())
                    .build();
        } catch (LoginException e) {
            logger.error("Invalid discord token", e);
            System.exit(-1);
            return;
        }
        CommandManager commandManager = CommandManager.create(jda);
        commandManager.registerListeners();
        commandManager.registerCommand(FusionCommand.withArguments("img", "Search images")
                .addOptions(GenericArguments.string("query", "Search query"),
                        GenericArguments.string("type", "File type").setRequired(false),
                        GenericArguments.integer("index", "Image index", 0, 9).setRequired(false))
                .setExecutor(new ImageCommand())
                .build());
        commandManager.registerCommand(FusionCommand.withArguments("youtube", "Search youtube videos")
                .addOptions(GenericArguments.string("query", "Search query"),
                        GenericArguments.string("type", "Type of resource. Default: video")
                                .addChoice("video", "video")
                                .addChoice("playlist", "playlist")
                                .addChoice("channel", "channel")
                                .setRequired(false),
                        GenericArguments.integer("index", "Video index", 0, 49).setRequired(false),
                        GenericArguments.integer("max", "Maximum result count", 1, 50).setRequired(false))
                .setExecutor(new YoutubeCommand())
                .build());
        Runtime.getRuntime().addShutdownHook(new Thread(jda::shutdown));
    }
}
