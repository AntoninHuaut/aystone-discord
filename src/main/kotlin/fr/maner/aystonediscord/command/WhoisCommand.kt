package fr.maner.aystonediscord.command

import fr.maner.aystonediscord.api.KeycloakAPI
import fr.maner.aystonediscord.api.PlayerDBApi
import fr.maner.aystonediscord.api.TwitchAPI
import fr.maner.aystonediscord.command.helper.CommandPermission
import fr.maner.aystonediscord.command.helper.EmbedBuilder
import fr.maner.aystonediscord.command.helper.KeycloakPlayerResolver
import fr.maner.aystonediscord.command.helper.SanctionButtonHandler
import fr.maner.aystonediscord.domain.Identities
import fr.maner.aystonediscord.domain.model.KeycloakPlayer
import fr.maner.aystonediscord.repository.AystonePlayerRepository
import fr.maner.aystonediscord.repository.AystoneSanctionRepository
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.components.ItemComponent

class WhoisCommand(
    private val jda: JDA,
    private val kcClient: KeycloakAPI,
    twClient: TwitchAPI,
    private val aystonePlayerRepository: AystonePlayerRepository,
    aystoneSanctionRepository: AystoneSanctionRepository,
    private val rolesId: List<String>,
) : ListenerAdapter() {

    private val keycloakPlayerResolver = KeycloakPlayerResolver(jda, kcClient, twClient)
    private val buttonHandler = SanctionButtonHandler(aystoneSanctionRepository, rolesId)

    companion object {
        const val NAME = "whois"
        const val DESCRIPTION = "Displays information about a player"
        const val CONTEXT_MENU_NAME = "Aystone Player Info"
    }

    object Options {
        data class Option(val name: String, val description: String, val type: OptionType)

        val DISCORD_ID_NAME = Option(Identities.DISCORD.getType(), "Discord id or name", OptionType.STRING)
        val MINECRAFT_NAME_UUID = Option(Identities.MINECRAFT.getType(), "Minecraft name or uuid", OptionType.STRING)
        val TWITCH_ID_NAME = Option(Identities.TWITCH.getType(), "Twitch id or name", OptionType.STRING)

        val ALL = listOf(DISCORD_ID_NAME, MINECRAFT_NAME_UUID, TWITCH_ID_NAME)
    }

    fun createSlashCommand(): SlashCommandData {
        val baseCommand = Commands.slash(NAME, DESCRIPTION).setContexts(InteractionContextType.GUILD)
        return Options.ALL.fold(baseCommand) { cmd, option ->
            cmd.addOption(option.type, option.name, option.description, false)
        }
    }

    fun createContextCommand(): CommandData {
        return Commands.user(CONTEXT_MENU_NAME).setContexts(InteractionContextType.GUILD)
    }

    override fun onUserContextInteraction(event: UserContextInteractionEvent) {
        if (event.name != CONTEXT_MENU_NAME) return
        if (!CommandPermission.hasPermission(event, event.member, rolesId)) return

        val discordAlias = Identities.DISCORD.getIdpAlias(kcClient.getIdentitiesMap()) ?: return
        val identities = kcClient.getFederatedIdentitiesByIdpId(discordAlias, event.target.id) ?: run {
            event.reply("❌ No Keycloak Player found for `${event.target.globalName}`.").setEphemeral(true).queue()
            return
        }

        displayWhois(event, KeycloakPlayer.from(identities))
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != NAME) return
        if (!CommandPermission.hasPermission(event, event.member, rolesId)) return

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
                val kPlayer = keycloakPlayerResolver.resolve(event, option.name, option.asString) ?: return
                displayWhois(event, kPlayer)
            }

            else -> {
                event.reply("❌ Please provide exactly one option, not multiple.").setEphemeral(true).queue()
            }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        buttonHandler.handleButtonInteraction(event)
    }

    private fun displayWhois(event: GenericCommandInteractionEvent, kPlayer: KeycloakPlayer) {
        val aPlayer = aystonePlayerRepository.getByUuid(kPlayer.mcUuid)
        if (aPlayer == null) {
            event.reply("❌ Aystone Player not found. The player may have never joined the server.").setEphemeral(true).queue()
            return
        }

        val mcInfo = PlayerDBApi.getByNameOrUuid(aPlayer.uuid.toString()) ?: run {
            event.reply("❌ Error while fetching Minecraft name for `${aPlayer.uuid}`.").setEphemeral(true).queue()
            return
        }

        val embed = EmbedBuilder.buildPlayerInfoEmbed(aPlayer, kPlayer, mcInfo.username)
        val buttons = buttonHandler.createSanctionButtons(kPlayer.mcUuid)

        event.replyEmbeds(embed)
            .setActionRow(*buttons.map { it as ItemComponent }.toTypedArray())
            .queue()
    }
}
