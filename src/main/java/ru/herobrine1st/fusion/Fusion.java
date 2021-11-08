package ru.herobrine1st.fusion;

import com.mysql.cj.jdbc.Driver;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.herobrine1st.fusion.api.command.GenericArguments;
import ru.herobrine1st.fusion.api.command.option.FusionCommand;
import ru.herobrine1st.fusion.api.command.option.FusionSubcommand;
import ru.herobrine1st.fusion.api.manager.CommandManager;
import ru.herobrine1st.fusion.command.ImageCommand;
import ru.herobrine1st.fusion.command.SubscribeToVkGroupCommand;
import ru.herobrine1st.fusion.command.YoutubeCommand;
import ru.herobrine1st.fusion.parser.URLParserElement;
import ru.herobrine1st.fusion.permission.OwnerPermissionHandler;
import ru.herobrine1st.fusion.tasks.VkGroupFetchTask;

import javax.persistence.Entity;
import javax.security.auth.login.LoginException;
import java.util.concurrent.TimeUnit;

public class Fusion {
    private static final Logger logger = LoggerFactory.getLogger("Fusion");
    private static SessionFactory sessionFactory;
    private static JDA jda;

    static {
        System.setProperty("org.jboss.logging.provider", "slf4j");
    }

    public static void main(String[] args) {
        try {
            jda = JDABuilder.createLight(Config.getDiscordToken())
                    .build();
        } catch (LoginException e) {
            logger.error("Invalid discord token", e);
            System.exit(-1);
            return;
        }

        Configuration configuration = new Configuration()
                .setProperty(Environment.URL, "jdbc:mysql://%s:%s@%s:%s/%s".formatted(
                        Config.getMysqlUsername(),
                        Config.getMysqlPassword(),
                        Config.getMysqlHost(),
                        Config.getMysqlPort(),
                        Config.getMysqlDatabase()
                ))
                .setProperty(Environment.DRIVER, Driver.class.getCanonicalName())
                .setProperty(Environment.HBM2DDL_AUTO, "update");
        new Reflections("ru.herobrine1st.fusion")
                .getTypesAnnotatedWith(Entity.class)
                .stream()
                .peek(clazz -> logger.trace("Registering entity %s in hibernate".formatted(clazz.getCanonicalName())))
                .forEach(configuration::addAnnotatedClass);
        sessionFactory = configuration.buildSessionFactory();

        Pools.SCHEDULED_POOL.scheduleAtFixedRate(new VkGroupFetchTask(), 30, 30, TimeUnit.MINUTES);

        CommandManager commandManager = CommandManager.create(jda);
        commandManager.registerListeners();
        commandManager.registerCommand(FusionCommand.withArguments("img", "Search images")
                .addOptions(GenericArguments.string("query", "Search query"),
                        GenericArguments.string("type", "File type").setRequired(false),
                        GenericArguments.integer("index", "Image index", 0, 9).setRequired(false),
                        GenericArguments.string("safe", "Whether to enable safe search")
                                .addChoice("No", "off")
                                .addChoice("Yes", "Active")
                                .setRequired(false))
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

        commandManager.registerCommand(FusionCommand.withSubcommands("vkgroup", "Manage VK group subscriptions to channel")
                .addOptions(FusionSubcommand.builder("subscribe", "Subscribe to VK group")
                        .setExecutor(new SubscribeToVkGroupCommand())
                        .addOptions(new URLParserElement("group", "Link to group").setHost("vk.com"))
                        .setPermissionHandler(new OwnerPermissionHandler())
                        .build())
                .build());

        commandManager.updateCommands().queue(null, Throwable::printStackTrace);

        Runtime.getRuntime().addShutdownHook(new Thread(jda::shutdown));
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static JDA getJda() {
        return jda;
    }
}
