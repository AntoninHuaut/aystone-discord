package fr.maner.aystonediscord.usecase

import fr.maner.aystonediscord.api.PlayerDBApi
import fr.maner.aystonediscord.domain.model.AystonePlayer
import fr.maner.aystonediscord.domain.model.AystoneSanction
import fr.maner.aystonediscord.domain.model.KeycloakPlayer
import fr.maner.aystonediscord.repository.AystonePlayerRepository
import fr.maner.aystonediscord.repository.AystoneSanctionRepository
import fr.maner.aystonediscord.usecase.helper.KeycloakPlayerRetrieve
import fr.maner.aystonediscord.usecase.helper.PaginatedEmbed
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.awt.Color
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

class WhoisCommand(
    private val aystonePlayerRepository: AystonePlayerRepository,
    private val aystoneSanctionRepository: AystoneSanctionRepository,
) : ListenerAdapter() {

    companion object {
        const val NAME = "whois"
        const val DESCRIPTION = "Displays information about a player"
        const val CONTEXT_MENU_NAME = "Aystone Player Info"

        private const val BUTTON_PREFIX_SANCTION_NOTHING = "whois_sanction_nothing"
        private const val BUTTON_PREFIX_SANCTION_ASK = "whois_sanction_ask"
        private const val BUTTON_PREFIX_SANCTION_SEE = "whois_sanction_see"
        private const val MINOTAR_URL = "https://minotar.net/avatar"

        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH)
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

        displayWhois(event, kPlayer)
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
                val option = providedOptions.first()
                val kPlayer = KeycloakPlayerRetrieve.byOption(option.name, option.asString) ?: run {
                    event.reply("❌ Invalid option provided: `${option.name}` with value `${option.asString}`.").setEphemeral(true).queue()
                    return
                }

                displayWhois(event, kPlayer)
            }

            else -> {
                event.reply("❌ Please provide exactly one option, not multiple.").setEphemeral(true).queue()
            }
        }
    }

    fun displayWhois(event: GenericCommandInteractionEvent, kPlayer: KeycloakPlayer) {
        val aPlayer = aystonePlayerRepository.getByUuid(kPlayer.mcUuid)
        if (aPlayer == null) {
            event.reply("❌ Aystone Player not found. The player may have never joined the server.").setEphemeral(true).queue()
            return
        }

        val mcInfo: String = runBlocking {
            try {
                val mcInfo = PlayerDBApi.getByNameOrUuid(aPlayer.uuid.toString()) ?: throw Exception("not found")
                mcInfo.username
            } catch (e: Exception) {
                event.reply("❌ Error while fetching Minecraft name for `${aPlayer.uuid}`: ${e.message}.").setEphemeral(true).queue()
                null
            }
        } ?: return

        val buttons = mutableListOf<Button>()
        val nbSanctions = aystoneSanctionRepository.countByUuid(kPlayer.mcUuid)

        if (nbSanctions > 0) {
            buttons.add(
                Button.primary(
                    "${BUTTON_PREFIX_SANCTION_ASK}:${kPlayer.mcUuid}",
                    if (nbSanctions > 1) "See the $nbSanctions sanctions" else "See the sanction"
                )
                    .withEmoji(Emoji.fromUnicode("⚠"))
            )
        } else {
            buttons.add(
                Button.secondary("${BUTTON_PREFIX_SANCTION_NOTHING}:${kPlayer.mcUuid}", "No sanction")
                    .withEmoji(Emoji.fromUnicode("✅"))
                    .asDisabled()
            )
        }

        event.replyEmbeds(toEmbed(aPlayer, kPlayer, mcInfo)).setActionRow(*buttons.map { it as ItemComponent }.toTypedArray()).queue()
    }

    fun toEmbed(aPlayer: AystonePlayer, kPlayer: KeycloakPlayer, mcName: String): MessageEmbed {
        return EmbedBuilder()
            .setColor(Color(0x1ABC9C))
            .setFooter("Aystone", null)
            .setThumbnail("$MINOTAR_URL/${aPlayer.uuid}.png")
            .setTimestamp(Instant.now())
            .setTitle("👤 Player Info: `${mcName}`")

            .addField("UUID", aPlayer.uuid.toString(), false)

            .addField("Whitelist", if (aPlayer.whitelist) "✅ Yes" else "❌ No", true)
            .addField("Banned", if (aPlayer.ban) "🚫 Yes" else "🟢 No", true)
            .addField("Instance", aPlayer.instanceName ?: "None", true)

            .addField("Created On", aPlayer.createdOn.format(DATE_FORMATTER), true)
            .addField("Last Login", aPlayer.lastLogin.format(DATE_FORMATTER), true)
            .addField("\u200B", "\u200B", true)

            .addField("Twitch", "TBD Twitch Name (`${kPlayer.twitchId}`)", true)
            .addField("Discord", "TBD Discord Name (`${kPlayer.discordId}`)", true)
            .build()
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val componentId = event.componentId

        if (componentId.startsWith(BUTTON_PREFIX_SANCTION_ASK)) {
            val uuidRaw = componentId.substringAfter("$BUTTON_PREFIX_SANCTION_ASK:")
            val uuid = try {
                UUID.fromString(uuidRaw)
            } catch (_: IllegalArgumentException) {
                event.reply("❌ Invalid UUID in button interaction.").setEphemeral(true).queue()
                return
            }

            val sanctions = aystoneSanctionRepository.getByUuidSortDateDesc(uuid)
            if (sanctions.isEmpty()) {
                event.reply("❌ No sanctions found.").setEphemeral(true).queue()
                return
            }

            val mcInfo: PlayerDBApi.PlayerInfo = runBlocking {
                try {
                    return@runBlocking PlayerDBApi.getByNameOrUuid(uuid.toString())
                } catch (e: Exception) {
                    event.reply("❌ Error while fetching Minecraft name for `$uuid`: ${e.message}.").setEphemeral(true).queue()
                    return@runBlocking null
                }
            } ?: return

            val (embed, buttons) = PaginatedEmbed.handleNewPagination(
                event, BUTTON_PREFIX_SANCTION_SEE, sanctions,
                title = "Sanction List: `${mcInfo.username}`",
                itemsPerPage = 9,
                fieldBuilder = { i, sanction, embed -> createFieldSanction(sanction, embed) },
                embedBuilder = {
                    it.setThumbnail("$MINOTAR_URL/${uuid}.png")
                    it.setFooter(
                        "Requested by ${event.user.name}",
                        event.user.effectiveAvatarUrl
                    )
                }
            )

            event.replyEmbeds(embed)
                .mentionRepliedUser(true)
                .setActionRow(*buttons.map { it as ItemComponent }.toTypedArray())
                .queue()
        } else if (componentId.startsWith(BUTTON_PREFIX_SANCTION_SEE)) {
            PaginatedEmbed.handleUpdatePagination(event, BUTTON_PREFIX_SANCTION_SEE)
        }
    }

    fun createFieldSanction(sanction: AystoneSanction, embed: EmbedBuilder): EmbedBuilder {
        return embed.addField(
            sanction.type.name,
            listOf(
                "*${sanction.sanctionApplied.format(DATE_FORMATTER)}*",
                "${sanction.reason}",
            ).joinToString("\n"),
            true
        )
    }

}