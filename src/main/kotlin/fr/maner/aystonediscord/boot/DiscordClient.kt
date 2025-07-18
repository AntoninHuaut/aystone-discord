package fr.maner.aystonediscord.boot

import fr.maner.aystonediscord.api.AystoneAPI
import fr.maner.aystonediscord.api.TwitchAPI
import fr.maner.aystonediscord.command.InstanceCommand
import fr.maner.aystonediscord.command.RecordCommand
import fr.maner.aystonediscord.command.WhoisCommand
import fr.maner.aystonediscord.command.helper.PaginatedEmbed
import fr.maner.aystonediscord.command.helper.SanctionButtonHandler
import fr.maner.aystonediscord.domain.BotConfig
import fr.maner.aystonediscord.repository.AystoneInstanceRepository
import fr.maner.aystonediscord.repository.AystonePlayerRepository
import fr.maner.aystonediscord.repository.AystoneSanctionRepository
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy

class DiscordClient(
    botConfig: BotConfig,
    kcClient: AystoneAPI,
    twClient: TwitchAPI,
    aInstanceRepo: AystoneInstanceRepository,
    aPlayerRepo: AystonePlayerRepository,
    aSanctionRepo: AystoneSanctionRepository,
) {

    private val jdaInstance: JDA = JDABuilder.createDefault(botConfig.token)
        .enableIntents(GatewayIntent.GUILD_MEMBERS)
        .setChunkingFilter(ChunkingFilter.ALL)
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .setActivity(Activity.playing(botConfig.activity))
        .build()

    private val sanctionButtonHandler = SanctionButtonHandler(aSanctionRepo, botConfig.rolesId)
    private val whoisCmd =
        WhoisCommand(jdaInstance, kcClient, aPlayerRepo, sanctionButtonHandler, twClient, botConfig.rolesId)
    private val recordCmd = RecordCommand(jdaInstance, kcClient, sanctionButtonHandler, twClient, botConfig.rolesId)
    private val instanceCmd = InstanceCommand(aInstanceRepo, botConfig.rolesId)

    init {
        jdaInstance.awaitReady()
        initCommands()
    }

    fun initCommands() {
        jdaInstance.guilds.forEach { guild ->
            guild.updateCommands().addCommands(
                *instanceCmd.createCommands().toTypedArray(),
                *recordCmd.createCommands().toTypedArray(),
                *whoisCmd.createCommands().toTypedArray(),
            ).queue()
        }
        jdaInstance.addEventListener(instanceCmd, recordCmd, whoisCmd, sanctionButtonHandler)
    }

    fun close() {
        PaginatedEmbed.shutdown()
        if (jdaInstance.status != JDA.Status.SHUTDOWN) {
            jdaInstance.shutdown()
            jdaInstance.awaitShutdown()
        }
    }

}