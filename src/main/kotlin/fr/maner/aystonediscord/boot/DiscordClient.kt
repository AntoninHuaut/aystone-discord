package fr.maner.aystonediscord.boot

import fr.maner.aystonediscord.domain.BotConfig
import fr.maner.aystonediscord.repository.AystonePlayerRepository
import fr.maner.aystonediscord.usecase.DiscordWhoisCommand
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity

class DiscordClient(private var botConfig: BotConfig, private val aystonePlayerRepository: AystonePlayerRepository) {

    private var jdaInstance: JDA = JDABuilder.createDefault(botConfig.token)
        .setActivity(Activity.playing(botConfig.activity))
        .build()

    init {
        jdaInstance.awaitReady()
        initCommands()
    }

    fun initCommands() {
        val whoisCmd = DiscordWhoisCommand(aystonePlayerRepository)
        jdaInstance.guilds.forEach { guild ->
            guild.updateCommands().addCommands(
                whoisCmd.createCommand()
            ).queue()
        }
        jdaInstance.addEventListener(whoisCmd)
    }

    fun close() {
        if (jdaInstance.status != JDA.Status.SHUTDOWN) {
            jdaInstance.shutdown()
            jdaInstance.awaitShutdown()
        }
    }

}