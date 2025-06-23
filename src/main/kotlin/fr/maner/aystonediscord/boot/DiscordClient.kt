package fr.maner.aystonediscord.boot

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity

class DiscordClient(private val token: String) {
    fun buildBot(activityDescription: String): JDA {
        return JDABuilder.createDefault(token)
            .setActivity(Activity.playing(activityDescription))
            .build()
    }
}