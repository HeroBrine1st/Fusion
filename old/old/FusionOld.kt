package ru.herobrine1st.fusion.old

import com.mysql.cj.jdbc.Driver
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import org.hibernate.cfg.Environment
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.Config
import ru.herobrine1st.fusion.Pools
import ru.herobrine1st.fusion.api.command.GenericArguments
import ru.herobrine1st.fusion.api.command.option.FusionCommand
import ru.herobrine1st.fusion.api.command.option.FusionSubcommand
import ru.herobrine1st.fusion.api.manager.CommandManager
import ru.herobrine1st.fusion.command.*
import ru.herobrine1st.fusion.old.command.*
import ru.herobrine1st.fusion.old.parser.URLParserElement
import ru.herobrine1st.fusion.permission.OwnerPermissionHandler
import ru.herobrine1st.fusion.tasks.VkGroupFetchTask
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.persistence.Entity
import javax.security.auth.login.LoginException
import kotlin.system.exitProcess

object FusionOld {
    @JvmStatic
    private val logger = LoggerFactory.getLogger("Fusion")

    @JvmStatic
    lateinit var sessionFactory: SessionFactory
        private set

    @JvmStatic
    lateinit var jda: JDA
        private set

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            jda = JDABuilder.createLight(Config.getDiscordToken())
                .build()
        } catch (e: LoginException) {
            logger.error("Invalid discord token", e)
            exitProcess(-1)
        }
        val configuration = Configuration()
            .setProperty(Environment.URL, "jdbc:mysql://%s:%s@%s:%s/%s".format(
                Config.getMysqlUsername(),
                Config.getMysqlPassword(),
                Config.getMysqlHost(),
                Config.getMysqlPort(),
                Config.getMysqlDatabase()
            ))
            .setProperty(Environment.DRIVER, Driver::class.java.canonicalName)
            .setProperty(Environment.HBM2DDL_AUTO, "update")
        Reflections("ru.herobrine1st.fusion")
            .getTypesAnnotatedWith(Entity::class.java)
            .stream()
            .peek { clazz: Class<*> -> logger.trace("Registering entity ${clazz.canonicalName} in hibernate") }
            .forEach { annotatedClass: Class<*> -> configuration.addAnnotatedClass(annotatedClass) }
        try {
            sessionFactory = configuration.buildSessionFactory()
        } catch (e: Throwable) {
            e.printStackTrace()
            exitProcess(-1)
        }
        Pools.SCHEDULED_POOL.scheduleAtFixedRate(VkGroupFetchTask(), 1, 30, TimeUnit.MINUTES)
        val commandManager = CommandManager.create(jda)
        commandManager.registerListeners()
        commandManager.registerCommand(
            FusionCommand.withArguments("img", "Search images")
                .addOptions(
                    GenericArguments.string("query", "Search query"),
                    GenericArguments.string("type", "File type").setRequired(false),
                    GenericArguments.integer("index", "Image index", 0, 9).setRequired(false),
                    GenericArguments.string("safe", "Whether to enable safe search")
                        .addChoice("No", "off")
                        .addChoice("Yes", "active")
                        .setRequired(false)
                )
                .setExecutor(ImageCommand())
                .build()
        )
        commandManager.registerCommand(
            FusionCommand.withArguments("youtube", "Search youtube videos")
                .addOptions(
                    GenericArguments.string("query", "Search query"),
                    GenericArguments.string("type", "Type of resource. Default: video")
                        .addChoice("video", "video")
                        .addChoice("playlist", "playlist")
                        .addChoice("channel", "channel")
                        .setRequired(false),
                    GenericArguments.integer("index", "Video index", 0, 49).setRequired(false),
                    GenericArguments.integer("max", "Maximum result count", 1, 50).setRequired(false)
                )
                .setExecutor(YoutubeCommand())
                .build()
        )
        commandManager.registerCommand(
            FusionCommand.withSubcommands("vk", "VK social network related commands")
                .addOptions(
                    FusionSubcommand.builder("subscribe", "Subscribe to VK group")
                        .setExecutor(SubscribeToVkGroupCommand())
                        .addOptions(URLParserElement("group", "Link to group").setHost("vk.com"))
                        .setPermissionHandler(OwnerPermissionHandler())
                        .build(),
                    FusionSubcommand.builder("unsubscribe", "Unsubscribe from VK group")
                        .setExecutor(UnsubscribeFromVkGroupCommand())
                        .addOptions(URLParserElement("group", "Link to group").setHost("vk.com").setRequired(false))
                        .setPermissionHandler(OwnerPermissionHandler())
                        .build())
                .build()
        )
        commandManager.registerCommand(FusionCommand.withArguments("debug", "debug")
            .setExecutor(DebugCommand())
            .build()
        )
        commandManager.updateCommands().queue(null) { obj: Throwable -> obj.printStackTrace() }
        val startup = Instant.now()
        Pools.SCHEDULED_POOL.scheduleAtFixedRate({
            val duration = Instant.now() - startup
            jda.presence.activity = Activity.playing("Uptime: %d:%02d:%02d"
                .format(duration.toDaysPart(), duration.toHoursPart(), duration.toMinutesPart()))
        }, 0, 1, TimeUnit.MINUTES)
        Runtime.getRuntime().addShutdownHook(Thread { jda.shutdown() })
    }

    init {
        System.setProperty("org.jboss.logging.provider", "slf4j")
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
    }
}