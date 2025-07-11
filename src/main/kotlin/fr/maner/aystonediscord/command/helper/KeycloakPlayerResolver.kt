package fr.maner.aystonediscord.command.helper

import fr.maner.aystonediscord.api.KeycloakAPI
import fr.maner.aystonediscord.api.PlayerDBApi
import fr.maner.aystonediscord.api.TwitchAPI
import fr.maner.aystonediscord.domain.Identities
import fr.maner.aystonediscord.domain.model.KeycloakPlayer
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import java.util.*

class KeycloakPlayerResolver(
    private val jda: JDA,
    private val kcClient: KeycloakAPI,
    private val twClient: TwitchAPI
) {

    fun resolve(event: GenericCommandInteractionEvent, optionName: String, optionValue: String): KeycloakPlayer? {
        return when (optionName) {
            Identities.DISCORD.getType() -> resolveByDiscord(event, optionValue)
            Identities.MINECRAFT.getType() -> resolveByMinecraft(event, optionValue)
            Identities.TWITCH.getType() -> resolveByTwitch(event, optionValue)
            else -> null
        }
    }

    private fun resolveByDiscord(event: GenericCommandInteractionEvent, optionValue: String): KeycloakPlayer? {
        return resolveByIdentityProvider(
            event = event,
            optionValue = optionValue,
            identity = Identities.DISCORD,
            resolveNameToId = { name ->
                jda.getUsersByName(name, true).firstOrNull()?.id
            }
        )
    }

    private fun resolveByMinecraft(event: GenericCommandInteractionEvent, optionValue: String): KeycloakPlayer? {
        val mcInfo = PlayerDBApi.getByNameOrUuid(optionValue) ?: run {
            event.replyError("Error while fetching Minecraft Name or UUID `$optionValue`.")
            return null
        }

        // TODO get KC
        return KeycloakPlayer("", "", UUID.fromString(mcInfo.id), "", "", "")
    }

    private fun resolveByTwitch(event: GenericCommandInteractionEvent, optionValue: String): KeycloakPlayer? {
        return resolveByIdentityProvider(
            event = event,
            optionValue = optionValue,
            identity = Identities.TWITCH,
            resolveNameToId = { name ->
                twClient.getUserId(name)
            }
        )
    }

    private fun resolveByIdentityProvider(
        event: GenericCommandInteractionEvent,
        optionValue: String,
        identity: Identities,
        resolveNameToId: (String) -> String?
    ): KeycloakPlayer? {
        val idpAlias = identity.getIdpAlias(kcClient.getIdentitiesMap())
            ?: return null

        val identityId = if (optionValue.isOnlyDigits()) {
            optionValue
        } else {
            resolveNameToId(optionValue) ?: run {
                event.replyError("Error while fetching ${identity.getType()} ID `$optionValue`.")
                return null
            }
        }

        val identities = kcClient.getFederatedIdentitiesByIdpId(idpAlias, identityId) ?: run {
            event.replyError("No Keycloak Player found for `$optionValue`.")
            return null
        }

        return KeycloakPlayer.from(identities)
    }

    private fun String.isOnlyDigits(): Boolean = this.all { it.isDigit() }

    private fun GenericCommandInteractionEvent.replyError(message: String) {
        this.reply("❌ $message").setEphemeral(true).queue()
    }
}