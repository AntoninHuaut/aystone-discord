package fr.maner.aystonediscord

import io.github.cdimascio.dotenv.dotenv
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity

fun main() {
    val dotenv = dotenv()
    val discordToken = dotenv["DISCORD_TOKEN"] ?: throw IllegalStateException("DISCORD_TOKEN environment variable not set")

    val jda = JDABuilder.createDefault(discordToken)
        .setActivity(Activity.playing("Kotlin Bot"))
        .build()
}