package fr.maner.aystonediscord.boot

import fr.maner.aystonediscord.domain.BotConfig
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity

class DiscordClient(private var botConfig: BotConfig) {

    private var jdaInstance: JDA = JDABuilder.createDefault(botConfig.token)
        .setActivity(Activity.playing(botConfig.activity))
        .build()

    init {
        jdaInstance.awaitReady()
    }

    fun close() {
        if (jdaInstance.status != JDA.Status.SHUTDOWN) {
            jdaInstance.shutdown()
            jdaInstance.awaitShutdown()
        }
    }

}