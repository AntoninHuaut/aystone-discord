package fr.maner.aystonediscord.command

import fr.maner.aystonediscord.api.AystoneAPI
import fr.maner.aystonediscord.api.PlayerDBApi
import fr.maner.aystonediscord.api.TwitchAPI
import fr.maner.aystonediscord.command.helper.AystonePlayerResolver
import fr.maner.aystonediscord.command.helper.EmbedBuilder
import fr.maner.aystonediscord.command.helper.SanctionButtonHandler
import fr.maner.aystonediscord.domain.Identities
import fr.maner.aystonediscord.domain.model.AypiPlayer
import fr.maner.aystonediscord.repository.AystonePlayerRepository
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.interactions.components.ItemComponent

class WhoisCommand(
    private val jda: JDA,
    private val kcClient: AystoneAPI,
    private val aystonePlayerRepository: AystonePlayerRepository,
    private val sanctionButtonHandler: SanctionButtonHandler,
    twClient: TwitchAPI,
    rolesId: List<String>,
) : AbstractCommand(
    "whois", "Displays information about a player", AystonePlayerResolver.defaultResolverOptions,
    "Aystone Player Info", rolesId
) {

    private val aystonePlayerResolver = AystonePlayerResolver(jda, kcClient, twClient)

    override fun onUserContextInteractionAfterPermission(event: UserContextInteractionEvent) {
        val identities = kcClient.getUserByIdpId(Identities.DISCORD.getType(), event.target.id) ?: run {
            event.reply("❌ No AypiPlayer found for `${event.target.globalName}`.").setEphemeral(true).queue()
            return
        }

        displayWhois(event, AypiPlayer.from(jda, identities))
    }

    override fun onSlashCommandInteractionAfterPermission(event: SlashCommandInteractionEvent) {
        val providedOptions = options.mapNotNull { option ->
            event.getOption(option.name)
        }

        when (providedOptions.size) {
            0 -> {
                event.reply("❌ Please provide exactly one option: ${options.joinToString(", ") { it.description.lowercase() }}.")
                    .setEphemeral(true).queue()
                return
            }

            1 -> {
                val option = providedOptions.first()
                val kPlayer = aystonePlayerResolver.resolve(event, option.name, option.asString) ?: return
                displayWhois(event, kPlayer)
            }

            else -> {
                event.reply("❌ Please provide exactly one option, not multiple.").setEphemeral(true).queue()
            }
        }
    }

    private fun displayWhois(event: GenericCommandInteractionEvent, kPlayer: AypiPlayer) {
        val aPlayer = aystonePlayerRepository.getByUuid(kPlayer.mcUuid)
        if (aPlayer == null) {
            event.reply("❌ Aystone Player not found. The player may have never joined the server.").setEphemeral(true)
                .queue()
            return
        }

        val mcInfo = PlayerDBApi.getByNameOrUuid(aPlayer.uuid.toString()) ?: run {
            event.reply("❌ Error while fetching Minecraft name for `${aPlayer.uuid}`.").setEphemeral(true).queue()
            return
        }

        val embed = EmbedBuilder.buildPlayerInfoEmbed(aPlayer, kPlayer, mcInfo.username)
        val buttons = sanctionButtonHandler.createSanctionButtons(kPlayer.mcUuid)

        event.replyEmbeds(embed)
            .setActionRow(*buttons.map { it as ItemComponent }.toTypedArray())
            .queue()
    }
}
