package fr.maner.aystonediscord.command

import fr.maner.aystonediscord.api.AystoneAPI
import fr.maner.aystonediscord.api.TwitchAPI
import fr.maner.aystonediscord.command.helper.AystonePlayerResolver
import fr.maner.aystonediscord.command.helper.SanctionButtonHandler
import fr.maner.aystonediscord.domain.Identities
import fr.maner.aystonediscord.domain.model.AypiPlayer
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent

class RecordCommand(
    private val jda: JDA,
    private val kcClient: AystoneAPI,
    private val sanctionButtonHandler: SanctionButtonHandler,
    twClient: TwitchAPI,
    rolesId: List<String>,
) : AbstractCommand(
    "record", "Displays records about a player", AystonePlayerResolver.defaultResolverOptions,
    "Aystone Player Records", rolesId
) {

    private val aystonePlayerResolver = AystonePlayerResolver(jda, kcClient, twClient)

    override fun onUserContextInteractionAfterPermission(event: UserContextInteractionEvent) {
        val identities = kcClient.getUserByIdpId(Identities.DISCORD.getType(), event.target.id) ?: run {
            event.reply("❌ No AypiPlayer found for `${event.target.globalName}`.").setEphemeral(true).queue()
            return
        }

        sanctionButtonHandler.sendRecordsPlayerUUID(event, AypiPlayer.from(jda, identities).mcUuid)
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
                sanctionButtonHandler.sendRecordsPlayerUUID(event, kPlayer.mcUuid)
            }

            else -> {
                event.reply("❌ Please provide exactly one option, not multiple.").setEphemeral(true).queue()
            }
        }
    }
}
