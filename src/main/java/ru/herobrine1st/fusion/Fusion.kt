package ru.herobrine1st.fusion

import com.mysql.cj.jdbc.Driver
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import org.hibernate.cfg.Environment
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.module.googlesearch.command.ImgSearchCommand
import ru.herobrine1st.fusion.module.googlesearch.command.YoutubeSearchCommand
import ru.herobrine1st.fusion.listener.ButtonInteractionListener
import ru.herobrine1st.fusion.listener.SlashCommandListener
import ru.herobrine1st.fusion.util.minus
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import javax.security.auth.login.LoginException
import kotlin.system.exitProcess

lateinit var jda: JDA
    private set
lateinit var sessionFactory: SessionFactory
    private set
private val logger = LoggerFactory.getLogger("Fusion")

fun main(args: Array<String>) {
    try {
        jda = JDABuilder.createLight(Config.discordToken, EnumSet.noneOf(GatewayIntent::class.java))
            .addEventListeners(SlashCommandListener, ButtonInteractionListener)
            .build()
    } catch (e: LoginException) {
        exitWithMessage("Invalid discord token", e)
    }
    jda.awaitReady()
    Runtime.getRuntime().addShutdownHook(Thread { jda.shutdown() })

    val flags = HashSet<String>()
    args.forEach {
        if (it.startsWith("--")) flags.add(it.substring(2))
        // else if (it.startsWith("-")) flags.addAll(it.substring(2).toCharArray().map { it1 -> it1.toString() })
    }
    if ("update-commands" in flags) {
        (if (Config.testingGuildId != null) jda.getGuildById(Config.testingGuildId)
            ?.updateCommands() ?: exitWithMessage("Invalid TESTING_GUILD_ID environment variable provided")
        else jda.updateCommands())
            .addCommands(ImgSearchCommand.commandData, YoutubeSearchCommand.commandData)
            .queue()
        if ("no-exit" !in flags) exitProcess(0)
    }

    val configuration = Configuration()
        .setProperty(
            Environment.URL,
            "jdbc:mysql://%s:%s@%s:%s/%s".format(
                Config.mysqlUsername,
                Config.mysqlPassword,
                Config.mysqlHost,
                Config.mysqlPort,
                Config.mysqlDatabase
            )
        )
        .setProperty(Environment.DRIVER, Driver::class.java.canonicalName)
        .setProperty(Environment.HBM2DDL_AUTO, "validate")
//    Reflections("ru.herobrine1st.fusion")
//        .getTypesAnnotatedWith(Entity::class.java)
//        .stream()
//        .peek { clazz: Class<*> -> logger.trace("Registering entity %s in hibernate".format(clazz.canonicalName)) }
//        .forEach { annotatedClass: Class<*>? -> configuration.addAnnotatedClass(annotatedClass) }
    try {
        sessionFactory = configuration.buildSessionFactory()
    } catch (t: Throwable) {
        t.printStackTrace()
        exitProcess(2)
    }

//        Pools.SCHEDULED_POOL.scheduleAtFixedRate(VkGroupFetchTask(), 1, 30, TimeUnit.MINUTES)
    val startup = Instant.now()
    Pools.SCHEDULED_POOL.scheduleAtFixedRate({
        jda.presence.activity = with(Instant.now() - startup) {
            Activity.playing("Uptime: %d:%02d:%02d".format(toDaysPart(), toHoursPart(), toMinutesPart()))
        }
    }, 0, 1, TimeUnit.MINUTES)
    logger.info("Started")
}

fun exitWithMessage(msg: String, throwable: Throwable? = null, exitCode: Int = 2): Nothing {
    logger.error(msg, throwable)
    exitProcess(exitCode)
}