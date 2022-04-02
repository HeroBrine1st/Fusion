package ru.herobrine1st.fusion.util

import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import java.time.Duration
import java.time.Instant

operator fun Instant.minus(other: Instant): Duration {
    return Duration.between(other, this) // start, end: in arithmetics larger (end) minus smaller (start) returns positive value
}

@JvmName("addChoicesString")
fun OptionData.addChoices(vararg choices: Pair<String, String>): OptionData {
    this.addChoices(choices.map { Command.Choice(it.first, it.second) })
    return this
}

//@JvmName("addChoicesDouble")
//fun OptionData.addChoices(vararg choices: Pair<String, Double>): OptionData {
//    this.addChoices(choices.map { Command.Choice(it.first, it.second) })
//    return this
//}
//
//@JvmName("addChoicesLong")
//fun OptionData.addChoices(vararg choices: Pair<String, Long>): OptionData {
//    this.addChoices(choices.map { Command.Choice(it.first, it.second) })
//    return this
//}