package ru.herobrine1st.fusion

import dev.minn.jda.ktx.events.CoroutineEventManager
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag.*
import org.slf4j.LoggerFactory
import ru.herobrine1st.fusion.database.DatabaseFactory
import ru.herobrine1st.fusion.listener.ButtonInteractionListener
import ru.herobrine1st.fusion.listener.SlashCommandListener
import ru.herobrine1st.fusion.module.googlesearch.command.ImgSearchCommand
import ru.herobrine1st.fusion.module.googlesearch.command.YoutubeSearchCommand
import ru.herobrine1st.fusion.module.spotify.registerSpotifyListener
import ru.herobrine1st.fusion.module.vk.command.VkCommand
import ru.herobrine1st.fusion.module.vk.task.registerVkTask
import ru.herobrine1st.fusion.util.minus
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.security.auth.login.LoginException
import kotlin.system.exitProcess


lateinit var jda: JDA
    private set
private val logger = LoggerFactory.getLogger("Fusion")

fun main(args: Array<String>) {
    try {
        jda = JDABuilder.create(Config.discordToken, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
            .setEventManager(CoroutineEventManager())
            .disableCache(ACTIVITY, VOICE_STATE, EMOJI, STICKER, CLIENT_STATUS, ONLINE_STATUS, SCHEDULED_EVENTS)
            .build()
    } catch (e: LoginException) {
        exitWithMessage("Invalid discord token", e)
    }
    try {
        DatabaseFactory.init(
            host = Config.mysqlHost,
            port = Config.mysqlPort,
            database = Config.mysqlDatabase,
            username = Config.mysqlUsername,
            password = Config.mysqlPassword,
            dbms = "mysql"
        )
    } catch (t: Throwable) {
        t.printStackTrace()
        exitProcess(2)
    }
    jda.awaitReady()
    Runtime.getRuntime().addShutdownHook(Thread { jda.shutdown() })

    val flags = HashSet<String>()
    args.forEach {
        if (it.startsWith("--")) flags.add(it.substring(2))
        // else if (it.startsWith("-")) flags.addAll(it.substring(2).toCharArray().map { it1 -> it1.toString() })
    }
    if ("update-commands" in flags) {
        updateCommands()
        if ("no-exit" !in flags) exitProcess(0)
    }

    registerVkTask()
    registerSpotifyListener()
    jda.addEventListener(SlashCommandListener, ButtonInteractionListener)

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

fun updateCommands() {
    (if (Config.testingGuildId != null) jda.getGuildById(Config.testingGuildId)
        ?.updateCommands() ?: exitWithMessage("Invalid TESTING_GUILD_ID environment variable provided")
    else jda.updateCommands())
        .addCommands(
            // googlesearch
            ImgSearchCommand.commandData,
            YoutubeSearchCommand.commandData,
            // vk
            VkCommand.commandData
        )
        .complete()
    logger.info("Updated commands " + if (Config.testingGuildId != null) "in guild ${Config.testingGuildId}" else "globally")
}