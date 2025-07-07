package fr.maner.aystonediscord.usecase

import fr.maner.aystonediscord.domain.model.AystoneInstance
import fr.maner.aystonediscord.repository.AystoneInstanceRepository
import fr.maner.aystonediscord.usecase.helper.PaginatedEmbed
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.components.ItemComponent

class InstanceCommand(
    private val aystoneInstanceRepository: AystoneInstanceRepository,
) : ListenerAdapter() {

    companion object {
        const val NAME = "instance"
        const val DESCRIPTION = "Instance command"
        val PERMISSION = Permission.MESSAGE_MANAGE

        const val LIST_NAME = "list"
        const val LIST_DESCRIPTION = "List all instances"

        private const val BUTTON_PREFIX = "instance_list"
    }

    fun createCommand(): SlashCommandData {
        return Commands.slash(NAME, DESCRIPTION)
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE))
            .setContexts(InteractionContextType.GUILD)
            .addSubcommands(
                SubcommandData(LIST_NAME, LIST_DESCRIPTION)
            )
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != NAME) return

        when (event.subcommandName) {
            LIST_NAME -> {
                listInstances(event)
            }

            else -> {
                event.reply("❌ Unknown subcommand.").setEphemeral(true).queue()
            }
        }
    }

    fun listInstances(event: SlashCommandInteractionEvent) {
        val instances = aystoneInstanceRepository.getAll()
        if (instances.isEmpty()) {
            event.reply("❌ No instances found.").setEphemeral(true).queue()
            return
        }

        val (embed, buttons) = PaginatedEmbed.handleNewPagination(
            event, BUTTON_PREFIX, instances,
            title = "Instance List",
            userPermission = PERMISSION,
            itemsPerPage = 9,
            fieldBuilder = { i, sanction, embed -> createFieldInstance(sanction, embed) },
        )

        event.replyEmbeds(embed)
            .setActionRow(*buttons.map { it as ItemComponent }.toTypedArray())
            .queue()
    }


    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        PaginatedEmbed.handleUpdatePagination(event, BUTTON_PREFIX)
    }

    fun createFieldInstance(instance: AystoneInstance, embed: EmbedBuilder): EmbedBuilder {
        return embed.addField(
            instance.name,
            listOf(
                "Max players: ${instance.maxPlayer}",
                if (instance.enabled) "✅ Enabled" else "❌ Disabled",
                if (instance.visible) "✅ Visible" else "❌ Invisible",
            ).joinToString("\n"),
            true
        )
    }
}