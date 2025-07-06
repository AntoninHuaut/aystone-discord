package fr.maner.aystonediscord.usecase

import fr.maner.aystonediscord.repository.AystoneInstanceRepository
import fr.maner.aystonediscord.usecase.helper.PaginatedEmbed
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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class InstanceCommand(
    private val aystoneInstanceRepository: AystoneInstanceRepository,
) : ListenerAdapter() {

    companion object {
        const val NAME = "instance"
        const val DESCRIPTION = "Instance command"

        const val LIST_NAME = "list"
        const val LIST_DESCRIPTION = "List all instances"

        private const val BUTTON_PREFIX = "instance_list"
        private const val PAGINATOR_TIMEOUT_MINUTES = 30L
        private const val CLEANUP_INTERVAL_MINUTES = 5L
    }

    private val activePaginators = mutableMapOf<String, PaginatorData>()
    private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor()

    data class PaginatorData(
        val paginator: PaginatedEmbed<*>,
        val createdAt: Long = System.currentTimeMillis()
    )

    init {
        cleanupExecutor.scheduleAtFixedRate({
            cleanupOldPaginators()
        }, PAGINATOR_TIMEOUT_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES)
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

        val paginatorKey = "${event.user.id}_${event.id}"
        val paginator = PaginatedEmbed(instances, "Instance List", itemsPerPage = 9, fieldBuilder = { i, instance, embed ->
            embed.addField("${if (instance.visible) "🟢" else "🟠"} ${instance.name}", "Max players: ${instance.maxPlayer}", true)
        })
        activePaginators[paginatorKey] = PaginatorData(paginator)

        val page = 0
        val embed = paginator.getEmbedPage(page)
        val buttons = paginator.getPaginationButtons(page, BUTTON_PREFIX, paginatorKey)

        event.replyEmbeds(embed)
            .setActionRow(*buttons.map { it as ItemComponent }.toTypedArray())
            .setEphemeral(true)
            .queue()
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val parts = event.componentId.split(":")
        if (parts.size < 4 || parts[0] != BUTTON_PREFIX) return

        val paginatorKey = parts[1]
        val action = parts[2]
        val currentPage = parts[3].toIntOrNull() ?: return

        val paginatorData = activePaginators[paginatorKey]
        if (paginatorData == null) {
            event.reply("❌ This pagination session has expired. Please run the command again.")
                .setEphemeral(true)
                .queue()
            return
        }

        val paginator = paginatorData.paginator
        when (val result = paginator.handlePaginationAction(action, currentPage, event)) {
            is PaginatedEmbed.PaginationResult.Success -> {
                val newPage = result.newPage
                val embed = paginator.getEmbedPage(newPage)
                val buttons = paginator.getPaginationButtons(currentPage = newPage, prefix = BUTTON_PREFIX, key = paginatorKey)

                event.editMessageEmbeds(embed)
                    .setActionRow(*buttons.map { it as ItemComponent }.toTypedArray())
                    .queue()
            }

            else -> {}
        }
    }

    private fun cleanupOldPaginators() {
        val cutoffTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30)
        val keysToRemove = activePaginators.filterValues { it.createdAt < cutoffTime }.keys
        keysToRemove.forEach { activePaginators.remove(it) }
    }

    fun shutdown() {
        cleanupExecutor.shutdown()
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow()
            }
        } catch (_: InterruptedException) {
            cleanupExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}