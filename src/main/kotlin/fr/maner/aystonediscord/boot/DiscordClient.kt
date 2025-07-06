package fr.maner.aystonediscord.boot

import fr.maner.aystonediscord.domain.BotConfig
import fr.maner.aystonediscord.repository.AystoneInstanceRepository
import fr.maner.aystonediscord.repository.AystonePlayerRepository
import fr.maner.aystonediscord.usecase.InstanceCommand
import fr.maner.aystonediscord.usecase.WhoisCommand
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity

class DiscordClient(
    botConfig: BotConfig,
    private val aystonePlayerRepository: AystonePlayerRepository,
    private val aystoneInstanceRepository: AystoneInstanceRepository
) {

    private val jdaInstance: JDA = JDABuilder.createDefault(botConfig.token)
        .setActivity(Activity.playing(botConfig.activity))
        .build()

    private val whoisCmd = WhoisCommand(aystonePlayerRepository)
    private val instanceCmd = InstanceCommand(aystoneInstanceRepository)

    init {
        jdaInstance.awaitReady()
        initCommands()
    }

    fun initCommands() {
        val whoisCmd = WhoisCommand(aystonePlayerRepository)
        val instanceCmd = InstanceCommand(aystoneInstanceRepository)

        jdaInstance.guilds.forEach { guild ->
            guild.updateCommands().addCommands(
                whoisCmd.createCommand(),
                instanceCmd.createCommand()
            ).queue()
        }
        jdaInstance.addEventListener(whoisCmd, instanceCmd)
    }

    fun close() {
        instanceCmd.shutdown()
        if (jdaInstance.status != JDA.Status.SHUTDOWN) {
            jdaInstance.shutdown()
            jdaInstance.awaitShutdown()
        }
    }

}