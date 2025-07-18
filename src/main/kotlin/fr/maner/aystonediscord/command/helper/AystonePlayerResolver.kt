package fr.maner.aystonediscord.command.helper

import fr.maner.aystonediscord.api.AystoneAPI
import fr.maner.aystonediscord.api.PlayerDBApi
import fr.maner.aystonediscord.api.TwitchAPI
import fr.maner.aystonediscord.command.AbstractCommand
import fr.maner.aystonediscord.domain.Identities
import fr.maner.aystonediscord.domain.model.AypiPlayer
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class AystonePlayerResolver(
    private val jda: JDA,
    private val kcClient: AystoneAPI,
    private val twClient: TwitchAPI
) {

    companion object {
        val defaultResolverOptions = listOf(
            AbstractCommand.Option(Identities.DISCORD.getType(), "Discord id or name", OptionType.STRING),
            AbstractCommand.Option(Identities.MICROSOFT.getType(), "Minecraft name or uuid", OptionType.STRING),
            AbstractCommand.Option(Identities.TWITCH.getType(), "Twitch id or name", OptionType.STRING),
        )
    }

    fun resolve(event: GenericCommandInteractionEvent, optionName: String, optionValue: String): AypiPlayer? {
        return when (optionName) {
            Identities.DISCORD.getType() -> resolveByDiscord(event, optionValue)
            Identities.MICROSOFT.getType() -> resolveByMinecraft(event, optionValue)
            Identities.TWITCH.getType() -> resolveByTwitch(event, optionValue)
            else -> null
        }
    }

    private fun resolveByDiscord(event: GenericCommandInteractionEvent, optionValue: String): AypiPlayer? {
        return resolveByIdentityProvider(
            event = event,
            optionValue = optionValue,
            identity = Identities.DISCORD,
            resolveNameToId = { name ->
                jda.getUsersByName(name, true).firstOrNull()?.id
            }
        )
    }

    private fun resolveByMinecraft(event: GenericCommandInteractionEvent, optionValue: String): AypiPlayer? {
        return resolveByIdentityProvider(
            event = event,
            optionValue = optionValue,
            identity = Identities.MICROSOFT,
            resolveNameToId = { name ->
                PlayerDBApi.getByNameOrUuid(optionValue)?.id
            }
        )
    }

    private fun resolveByTwitch(event: GenericCommandInteractionEvent, optionValue: String): AypiPlayer? {
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
    ): AypiPlayer? {
        val identityId = if (optionValue.isOnlyDigits()) {
            optionValue
        } else {
            resolveNameToId(optionValue) ?: run {
                event.replyError("Error while fetching ${identity.getType()} ID `$optionValue`.")
                return null
            }
        }

        val identities = kcClient.getUserByIdpId(identity.getType(), identityId) ?: run {
            event.replyError("No AypiPlayer found for `$optionValue`.")
            return null
        }

        return AypiPlayer.from(identities)
    }

    private fun String.isOnlyDigits(): Boolean = this.all { it.isDigit() }

    private fun GenericCommandInteractionEvent.replyError(message: String) {
        this.reply("❌ $message").setEphemeral(true).queue()
    }
}