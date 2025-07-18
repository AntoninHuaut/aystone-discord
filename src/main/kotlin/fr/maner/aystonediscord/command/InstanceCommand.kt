package fr.maner.aystonediscord.command

import fr.maner.aystonediscord.command.helper.EmbedBuilder
import fr.maner.aystonediscord.command.helper.PaginatedEmbed
import fr.maner.aystonediscord.repository.AystoneInstanceRepository
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.components.ItemComponent

class InstanceCommand(
    private val aystoneInstanceRepository: AystoneInstanceRepository,
    rolesId: List<String>,
) : AbstractCommand(
    "instance", "Instance command",
    listOf(),
    null,
    rolesId
) {

    companion object {
        const val NAME = "instance"
        const val DESCRIPTION = "Instance command"

        const val LIST_NAME = "list"
        const val LIST_DESCRIPTION = "List all instances"

        private const val BUTTON_PREFIX = "instance_list"
    }

    override fun extendSlashCommand(slashCommand: SlashCommandData): SlashCommandData {
        return slashCommand.addSubcommands(SubcommandData(LIST_NAME, LIST_DESCRIPTION))
    }

    override fun onUserContextInteractionAfterPermission(event: UserContextInteractionEvent) {
        return
    }
    
    override fun onSlashCommandInteractionAfterPermission(event: SlashCommandInteractionEvent) {
        when (event.subcommandName) {
            LIST_NAME -> {
                listInstances(event)
            }

            else -> {
                event.reply("❌ Unknown subcommand.").setEphemeral(true).queue()
            }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        PaginatedEmbed.handleUpdatePaginationAndPermission(event, BUTTON_PREFIX, rolesId)
    }

    private fun listInstances(event: SlashCommandInteractionEvent) {
        val instances = aystoneInstanceRepository.getAll()
        if (instances.isEmpty()) {
            event.reply("❌ No instances found.").setEphemeral(true).queue()
            return
        }

        val (embed, buttons) = PaginatedEmbed.handleNewPagination(
            event, BUTTON_PREFIX, instances,
            title = "\uD83D\uDCCB Instance List", // 📋
            itemsPerPage = 9,
            fieldBuilder = { i, sanction, embed -> EmbedBuilder.buildInstanceField(sanction, embed) },
        )

        event.replyEmbeds(embed).setActionRow(*buttons.map { it as ItemComponent }.toTypedArray()).queue()
    }
}