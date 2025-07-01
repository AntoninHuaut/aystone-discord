// Option 1: Pure Kotlin version
package fr.maner.aystonediscord.usecase

import fr.maner.aystonediscord.api.MojangAPI
import fr.maner.aystonediscord.domain.model.AystonePlayer
import fr.maner.aystonediscord.domain.model.KeycloakPlayer
import fr.maner.aystonediscord.repository.AystonePlayerRepository
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.awt.Color
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

class DiscordWhoisCommand(
    private val aystonePlayerRepository: AystonePlayerRepository,
) : ListenerAdapter() {

    companion object {
        const val NAME = "whois"
        const val DESCRIPTION = "Displays information about a player"

        const val MINOTAR_URL = "https://minotar.net/avatar"
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
        val baseCommand = Commands.slash(NAME, DESCRIPTION)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
            .setContexts(InteractionContextType.GUILD)

        return Options.ALL.fold(baseCommand) { cmd, option ->
            cmd.addOption(option.type, option.name, option.description, false)
        }
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
            Options.TWITCH_ID.name -> {
                val twitchId = option.asString
                // TODO get KeycloakPlayer by ID Twitch
                val keycloakPlayer = KeycloakPlayer("", UUID.randomUUID(), twitchId)
                keycloakPlayer to getAystonePlayer(keycloakPlayer)
            }

            Options.TWITCH_NAME.name -> {
                val twitchName = option.asString
                // TODO get Twitch ID by name
                // TODO get KeycloakPlayer by ID Twitch
                val keycloakPlayer = KeycloakPlayer("", UUID.randomUUID(), "")
                keycloakPlayer to getAystonePlayer(keycloakPlayer)
            }

            Options.DISCORD_ID.name -> {
                val discordId = option.asString
                // TODO get KeycloakPlayer by ID Discord
                val keycloakPlayer = KeycloakPlayer(discordId, UUID.randomUUID(), "")
                keycloakPlayer to getAystonePlayer(keycloakPlayer)
            }

            Options.DISCORD_MENTION.name -> {
                val discordId = option.asUser.id
                // TODO get KeycloakPlayer by mention Discord
                val keycloakPlayer = KeycloakPlayer(discordId, UUID.randomUUID(), "")
                keycloakPlayer to getAystonePlayer(keycloakPlayer)
            }

            Options.MC_UUID.name -> {
                val mcUuidRaw = option.asString
                val mcUuid: UUID? = try {
                    UUID.fromString(mcUuidRaw)
                } catch (_: IllegalArgumentException) {
                    null
                }
                if (mcUuid == null) {
                    event.reply("❌ Invalid UUID: `$mcUuidRaw`.").setEphemeral(true).queue()
                    return
                }

                // TODO get KeycloakPlayer by UUID Minecraft
                val keycloakPlayer = KeycloakPlayer("", mcUuid, "")
                keycloakPlayer to getAystonePlayer(keycloakPlayer)
            }

            Options.MC_NAME.name -> {
                val mcName = option.asString
                val mcUUID: UUID? = try {
                    MojangAPI.getUUID(mcName)
                } catch (e: Exception) {
                    event.reply("❌ Erreur lors de la récupération du UUID Minecraft pour `$mcName`: ${e.message}").setEphemeral(true).queue()
                    return
                }

                if (mcUUID == null) {
                    event.reply("❌ Joueur Minecraft `$mcName` introuvable.").setEphemeral(true).queue()
                    return
                }

                // TODO get KeycloakPlayer by UUID Minecraft
                val keycloakPlayer = KeycloakPlayer("", mcUUID, "")
                keycloakPlayer to getAystonePlayer(keycloakPlayer)
            }

            else -> null to null
        }

        if (keycloakPlayer == null) {
            event.reply("❌ Keycloak Player not found for the provided option: `${option.name}` with value `${option.asString}`.").setEphemeral(true).queue()
            return
        }

        if (aystonePlayer == null) {
            event.reply("❌ Aystone Player not found for the provided option: `${option.name}` with value `${option.asString}`.").setEphemeral(true).queue()
            return
        }

        event.replyEmbeds(toEmbed(aystonePlayer, keycloakPlayer)).setEphemeral(true).queue()
    }

    private fun getAystonePlayer(keycloakPlayer: KeycloakPlayer): AystonePlayer? {
        return aystonePlayerRepository.getByUuid(keycloakPlayer.mcUuid)
    }

    fun toEmbed(aPlayer: AystonePlayer, kPlayer: KeycloakPlayer): MessageEmbed {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH)

        val embed = EmbedBuilder()
            .setColor(Color(0x1ABC9C))
            .setFooter("Aystone", null)
            .setThumbnail("$MINOTAR_URL/${aPlayer.uuid}.png")
            .setTimestamp(Instant.now())
            .setTitle("👤 Player Info")
            
            .addField("UUID", aPlayer.uuid.toString(), false)

            .addField("Whitelist", if (aPlayer.whitelist) "✅ Yes" else "❌ No", true)
            .addField("Banned", if (aPlayer.ban) "🚫 Yes" else "🟢 No", true)
            .addField("Instance", aPlayer.instanceName ?: "None", true)

            .addField("Created On", aPlayer.createdOn.format(formatter), true)
            .addField("Last Login", aPlayer.lastLogin.format(formatter), true)
            .addField("\u200B", "\u200B", true)

            .addField("Twitch", "TBD Twitch Name (`${kPlayer.twitchId}`)", true)
            .addField("Discord", "TBD Discord Name (`${kPlayer.discordId}`)", true)

        return embed.build()
    }
}