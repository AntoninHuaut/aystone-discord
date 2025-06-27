// Option 1: Pure Kotlin version
package fr.maner.aystonediscord.usecase

import fr.maner.aystonediscord.domain.model.AystonePlayer
import fr.maner.aystonediscord.domain.model.KeycloakPlayer
import fr.maner.aystonediscord.repository.AystonePlayerRepository
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.util.*

class DiscordWhoisCommand(private val aystonePlayerRepository: AystonePlayerRepository) : ListenerAdapter() {

    companion object {
        const val NAME = "whois"
        const val DESCRIPTION = "Displays information about a player"
    }

    object Options {
        data class Option(val name: String, val description: String, val type: OptionType)

        val DISCORD_MENTION = Option("discord_mention", "Discord user to display", OptionType.USER)
        val DISCORD_ID = Option("discord_id", "Discord id to display", OptionType.STRING)
        val MC_UUID = Option("mc_uuid", "Minecraft uuid to display", OptionType.STRING)
        val MC_NAME = Option("mc_name", "Minecraft name to display", OptionType.STRING)
        val TWITCH_ID = Option("twitch_id", "Twitch id to display", OptionType.STRING)
        val TWITCH_NAME = Option("twitch_name", "Twitch name to display", OptionType.STRING)

        val ALL = listOf(DISCORD_MENTION, DISCORD_ID, MC_UUID, MC_NAME, TWITCH_ID, TWITCH_NAME)
    }

    fun createCommand(): SlashCommandData {
        var command = Commands.slash(NAME, DESCRIPTION)
        Options.ALL.forEach { option ->
            command = command.addOption(option.type, option.name, option.description, false)
        }
        return command
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name == NAME) {
            val providedOptions = Options.ALL.mapNotNull { option ->
                event.getOption(option.name)
            }

            when (providedOptions.size) {
                0 -> {
                    event.reply("❌ Please provide exactly one option: ${Options.ALL.joinToString(", ") { it.description.lowercase() }}.")
                        .setEphemeral(true).queue()
                    return
                }

                1 -> {
                    handleSingleOption(event, providedOptions.first())
                }

                else -> {
                    event.reply("❌ Please provide exactly one option, not multiple.")
                        .setEphemeral(true).queue()
                }
            }
        }
    }

    private fun handleSingleOption(event: SlashCommandInteractionEvent, option: OptionMapping) {
        val (keycloakPlayer, aystonePlayer) = when (option.name) {
            Options.DISCORD_MENTION.name -> {
                val discordId = option.asUser.id
                // TODO get KeycloakPlayer by mention Discord
                val keycloakPlayer = KeycloakPlayer(discordId, UUID.randomUUID(), "")
                keycloakPlayer to getAystonePlayer(keycloakPlayer)
            }

            Options.DISCORD_ID.name -> {
                val discordId = option.asString
                // TODO get KeycloakPlayer by ID Discord
                val keycloakPlayer = KeycloakPlayer(discordId, UUID.randomUUID(), "")
                keycloakPlayer to getAystonePlayer(keycloakPlayer)
            }

            Options.MC_UUID.name -> {
                val mcUuid = option.asString
                // TODO get KeycloakPlayer by UUID Minecraft
                val keycloakPlayer = KeycloakPlayer("", UUID.fromString(mcUuid), "")
                keycloakPlayer to getAystonePlayer(keycloakPlayer)
            }

            Options.MC_NAME.name -> {
                val mcName = option.asString
                // TODO get KeycloakPlayer by nom Minecraft
                val keycloakPlayer = KeycloakPlayer("", UUID.randomUUID(), mcName)
                keycloakPlayer to getAystonePlayer(keycloakPlayer)
            }

            else -> null to null
        }

        if (keycloakPlayer == null) {
            event.reply("❌ Keycloak Player not found for the provided option: `${option.name}` with value `${option.asString}`.")
                .setEphemeral(true).queue()
            return
        }

        if (aystonePlayer == null) {
            event.reply("❌ Aystone Player not found for the provided option: `${option.name}` with value `${option.asString}`.")
                .setEphemeral(true).queue()
            return
        }

        event.reply(
            "✅ TwitchId: `${keycloakPlayer.twitchId}`\n" +
                    "DiscordId: `${keycloakPlayer.discordId}`\n" +
                    "Minecraft UUID: `${keycloakPlayer.mcUuid}`\n" +
                    "Instance: `${aystonePlayer.instanceName}`\n" +
                    "Last Seen: `${aystonePlayer.lastLogin}`\n" +
                    "Ban: `${aystonePlayer.ban}`\n" +
                    "Whitelist: `${aystonePlayer.whitelist}`\n"
        )
            .setEphemeral(true).queue()
    }

    private fun getAystonePlayer(keycloakPlayer: KeycloakPlayer): AystonePlayer? {
        return aystonePlayerRepository.getByUuid(keycloakPlayer.mcUuid)
    }
}