package fr.maner.aystonediscord.usecase

import fr.maner.aystonediscord.api.MinecraftAPI
import fr.maner.aystonediscord.domain.model.AystonePlayer
import fr.maner.aystonediscord.domain.model.KeycloakPlayer
import fr.maner.aystonediscord.repository.AystonePlayerRepository
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import java.awt.Color
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

class WhoisCommand(
    private val aystonePlayerRepository: AystonePlayerRepository,
) : ListenerAdapter() {

    companion object {
        const val NAME = "whois"
        const val DESCRIPTION = "Displays information about a player"
        const val CONTEXT_MENU_NAME = "Aystone Player Info"

        const val MINOTAR_URL = "https://minotar.net/avatar"
    }

    object Options {
        data class Option(val name: String, val description: String, val type: OptionType)

        val MC_UUID = Option("mc_uuid", "Minecraft uuid to display", OptionType.STRING)
        val MC_NAME = Option("mc_name", "Minecraft name to display", OptionType.STRING)
        val TWITCH_ID = Option("twitch_id", "Twitch id to display", OptionType.STRING)
        val TWITCH_NAME = Option("twitch_name", "Twitch name to display", OptionType.STRING)

        val ALL = listOf(MC_UUID, MC_NAME, TWITCH_ID, TWITCH_NAME)
    }

    fun createSlashCommand(): SlashCommandData {
        val baseCommand = Commands.slash(NAME, DESCRIPTION)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
            .setContexts(InteractionContextType.GUILD)

        return Options.ALL.fold(baseCommand) { cmd, option ->
            cmd.addOption(option.type, option.name, option.description, false)
        }
    }

    fun createContextCommand(): CommandData {
        return Commands.user(CONTEXT_MENU_NAME)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
            .setContexts(InteractionContextType.GUILD)
    }

    override fun onUserContextInteraction(event: UserContextInteractionEvent) {
        if (event.name != CONTEXT_MENU_NAME) return

        val targetUser = event.target
        val discordId = targetUser.id

        // TODO get KeycloakPlayer by Discord ID
        val kPlayer = KeycloakPlayer(discordId, UUID.fromString("b5238882-0706-49c2-992d-538ab1b057f6"), "")
        val aPlayer = getAystonePlayer(kPlayer)

        if (aPlayer == null) {
            event.reply("❌ No Aystone player found for ${targetUser.asMention}.").setEphemeral(true).queue()
            return
        }

        retrieveInfoAndSendEmbed(event, aPlayer, kPlayer)
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != NAME) return

        val providedOptions = Options.ALL.mapNotNull { option ->
            event.getOption(option.name)
        }

        when (providedOptions.size) {
            0 -> {
                event.reply("❌ Please provide exactly one option: ${Options.ALL.joinToString(", ") { it.description.lowercase() }}.").setEphemeral(true).queue()
                return
            }

            1 -> {
                handleSingleOption(event, providedOptions.first())
            }

            else -> {
                event.reply("❌ Please provide exactly one option, not multiple.").setEphemeral(true).queue()
            }
        }
    }

    private fun handleSingleOption(event: SlashCommandInteractionEvent, option: OptionMapping) {
        val kPlayer = when (option.name) {
            Options.TWITCH_ID.name -> {
                val twitchId = option.asString
                // TODO get KeycloakPlayer by ID Twitch
                KeycloakPlayer("", UUID.randomUUID(), twitchId)
            }

            Options.TWITCH_NAME.name -> {
                val twitchName = option.asString
                // TODO get Twitch ID by name
                // TODO get KeycloakPlayer by ID Twitch
                KeycloakPlayer("", UUID.randomUUID(), "")
            }

            Options.MC_UUID.name -> {
                val mcUuidRaw = option.asString
                val mcUuid: UUID = try {
                    UUID.fromString(mcUuidRaw) ?: throw IllegalArgumentException("Invalid UUID format")
                } catch (_: IllegalArgumentException) {
                    event.reply("❌ Invalid UUID: `$mcUuidRaw`.").setEphemeral(true).queue()
                    null
                } ?: return

                // TODO get KeycloakPlayer by UUID Minecraft
                KeycloakPlayer("", mcUuid, "")
            }

            Options.MC_NAME.name -> {
                val mcName = option.asString
                val mcUUID: UUID = runBlocking {
                    try {
                        MinecraftAPI.getUUID(mcName) ?: throw Exception("not found")
                    } catch (e: Exception) {
                        event.reply("❌ Error while fetching Minecraft UUID for `$mcName`: ${e.message}").setEphemeral(true).queue()
                        null
                    }
                } ?: return

                // TODO get KeycloakPlayer by UUID Minecraft
                KeycloakPlayer("", mcUUID, "")
            }

            else -> null
        }

        if (kPlayer == null) {
            event.reply("❌ Keycloak Player not found for the provided option: `${option.name}` with value `${option.asString}`.").setEphemeral(true).queue()
            return
        }

        val aPlayer = getAystonePlayer(kPlayer)
        if (aPlayer == null) {
            event.reply("❌ Aystone Player not found for the provided option: `${option.name}` with value `${option.asString}`.").setEphemeral(true).queue()
            return
        }

        retrieveInfoAndSendEmbed(event, aPlayer, kPlayer)
    }

    fun retrieveInfoAndSendEmbed(event: GenericCommandInteractionEvent, aPlayer: AystonePlayer, kPlayer: KeycloakPlayer) {
        val mcName: String = runBlocking {
            try {
                MinecraftAPI.getName(aPlayer.uuid) ?: throw Exception("not found")
            } catch (e: Exception) {
                event.reply("❌ Error while fetching Minecraft name for `${aPlayer.uuid}`: ${e.message}").setEphemeral(true).queue()
                null
            }
        } ?: return

        event.replyEmbeds(toEmbed(aPlayer, kPlayer, mcName)).queue()
    }

    private fun getAystonePlayer(keycloakPlayer: KeycloakPlayer): AystonePlayer? {
        return aystonePlayerRepository.getByUuid(keycloakPlayer.mcUuid)
    }

    fun toEmbed(aPlayer: AystonePlayer, kPlayer: KeycloakPlayer, mcName: String): MessageEmbed {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH)

        val embed = EmbedBuilder()
            .setColor(Color(0x1ABC9C))
            .setFooter("Aystone", null)
            .setThumbnail("$MINOTAR_URL/${aPlayer.uuid}.png")
            .setTimestamp(Instant.now())
            .setTitle("👤 Player Info: `${mcName}`")

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