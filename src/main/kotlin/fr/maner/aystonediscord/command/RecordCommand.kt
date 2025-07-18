package fr.maner.aystonediscord.command

import fr.maner.aystonediscord.api.KeycloakAPI
import fr.maner.aystonediscord.api.TwitchAPI
import fr.maner.aystonediscord.command.helper.KeycloakPlayerResolver
import fr.maner.aystonediscord.command.helper.SanctionButtonHandler
import fr.maner.aystonediscord.domain.Identities
import fr.maner.aystonediscord.domain.model.KeycloakPlayer
import fr.maner.aystonediscord.repository.AystoneSanctionRepository
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent

class RecordCommand(
    private val jda: JDA,
    private val kcClient: KeycloakAPI,
    private val sanctionButtonHandler: SanctionButtonHandler,
    twClient: TwitchAPI,
    aystoneSanctionRepository: AystoneSanctionRepository,
    rolesId: List<String>,
) : AbstractCommand(
    "record", "Displays records about a player", KeycloakPlayerResolver.defaultResolverOptions,
    "Aystone Player Records", rolesId
) {

    private val keycloakPlayerResolver = KeycloakPlayerResolver(jda, kcClient, twClient)

    override fun onUserContextInteractionAfterPermission(event: UserContextInteractionEvent) {
        val discordAlias = Identities.DISCORD.getIdpAlias(kcClient.getIdentitiesMap()) ?: return
        val identities = kcClient.getFederatedIdentitiesByIdpId(discordAlias, event.target.id) ?: run {
            event.reply("❌ No Keycloak Player found for `${event.target.globalName}`.").setEphemeral(true).queue()
            return
        }

        sanctionButtonHandler.sendRecordsPlayerUUID(event, KeycloakPlayer.from(identities).mcUuid)
    }

    override fun onSlashCommandInteractionAfterPermission(event: SlashCommandInteractionEvent) {
        val providedOptions = options.mapNotNull { option ->
            event.getOption(option.name)
        }

        when (providedOptions.size) {
            0 -> {
                event.reply("❌ Please provide exactly one option: ${options.joinToString(", ") { it.description.lowercase() }}.").setEphemeral(true).queue()
                return
            }

            1 -> {
                val option = providedOptions.first()
                val kPlayer = keycloakPlayerResolver.resolve(event, option.name, option.asString) ?: return
                sanctionButtonHandler.sendRecordsPlayerUUID(event, kPlayer.mcUuid)
            }

            else -> {
                event.reply("❌ Please provide exactly one option, not multiple.").setEphemeral(true).queue()
            }
        }
    }
}
