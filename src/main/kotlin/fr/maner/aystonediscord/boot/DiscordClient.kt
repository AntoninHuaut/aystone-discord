package fr.maner.aystonediscord.boot

import fr.maner.aystonediscord.command.InstanceCommand
import fr.maner.aystonediscord.command.WhoisCommand
import fr.maner.aystonediscord.command.helper.PaginatedEmbed
import fr.maner.aystonediscord.domain.BotConfig
import fr.maner.aystonediscord.repository.AystoneInstanceRepository
import fr.maner.aystonediscord.repository.AystonePlayerRepository
import fr.maner.aystonediscord.repository.AystoneSanctionRepository
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity

class DiscordClient(
    botConfig: BotConfig,
    private val aystoneInstanceRepository: AystoneInstanceRepository,
    private val aystonePlayerRepository: AystonePlayerRepository,
    private val aystoneSanctionRepository: AystoneSanctionRepository,
) {

    private val jdaInstance: JDA = JDABuilder.createDefault(botConfig.token)
        .setActivity(Activity.playing(botConfig.activity))
        .build()

    private val whoisCmd = WhoisCommand(aystonePlayerRepository, aystoneSanctionRepository, botConfig.rolesId)
    private val instanceCmd = InstanceCommand(aystoneInstanceRepository, botConfig.rolesId)

    init {
        jdaInstance.awaitReady()
        initCommands()
    }

    fun initCommands() {
        jdaInstance.guilds.forEach { guild ->
            guild.updateCommands().addCommands(
                whoisCmd.createSlashCommand(),
                whoisCmd.createContextCommand(),
                instanceCmd.createCommand()
            ).queue()
        }
        jdaInstance.addEventListener(whoisCmd, instanceCmd)
    }

    fun close() {
        PaginatedEmbed.shutdown()
        if (jdaInstance.status != JDA.Status.SHUTDOWN) {
            jdaInstance.shutdown()
            jdaInstance.awaitShutdown()
        }
    }

}