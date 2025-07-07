package fr.maner.aystonediscord.usecase.helper

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class PaginationEmojis(
    val first: String = "⏮\uFE0F",
    val previous: String = "⏪",
    val next: String = "⏩",
    val last: String = "⏭\uFE0F"
)

sealed class PaginationResult {
    data class Success(val newPage: Int) : PaginationResult()
    object NoAction : PaginationResult()
    object Error : PaginationResult()
}

data class PaginatorData(
    val paginator: PaginatedEmbed<*>,
    val createdAt: Long = System.currentTimeMillis()
)

class PaginatedEmbed<T>(
    private val items: List<T>,
    private val title: String = "Paginated List",
    private val itemsPerPage: Int = 5,
    private val embedBuilder: (EmbedBuilder) -> EmbedBuilder = { it },
    private val fieldBuilder: (Int, T, EmbedBuilder) -> EmbedBuilder,
) {
    private val totalPages = (items.size + itemsPerPage - 1) / itemsPerPage

    companion object {
        private const val ADDITIONAL_BUTTONS_MIN_PAGE = 3
        private const val PAGINATOR_TIMEOUT_MINUTES = 30L
        private const val CLEANUP_INTERVAL_MINUTES = 5L

        val activePaginators = mutableMapOf<String, PaginatorData>()
        private val cleanupExecutor = Executors.newSingleThreadScheduledExecutor()

        init {
            cleanupExecutor.scheduleAtFixedRate({
                cleanupOldPaginators()
            }, PAGINATOR_TIMEOUT_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES)
        }

        fun <T> handleNewPagination(
            event: GenericInteractionCreateEvent,
            btnPrefix: String,
            items: List<T>,
            title: String = "Paginated List",
            itemsPerPage: Int = 5,
            embedBuilder: (EmbedBuilder) -> EmbedBuilder = { it },
            fieldBuilder: (Int, T, EmbedBuilder) -> EmbedBuilder,
        ): Pair<MessageEmbed, List<Button>> {
            val paginatorKey = "${event.user.id}_${event.id}"
            val paginator = PaginatedEmbed(
                items = items,
                title = title,
                itemsPerPage = itemsPerPage,
                embedBuilder = embedBuilder,
                fieldBuilder = fieldBuilder
            )

            activePaginators[paginatorKey] = PaginatorData(paginator)

            val initialPage = 0
            val embed = paginator.getEmbedPage(initialPage)
            val buttons = paginator.getPaginationButtons(currentPage = initialPage, prefix = btnPrefix, key = paginatorKey)

            return embed to buttons
        }

        fun handleUpdatePagination(
            event: ButtonInteractionEvent,
            btnPrefix: String,
        ) {
            val parts = event.componentId.split(":")
            if (parts.size < 4) return

            val prefix = parts[0]
            if (prefix != btnPrefix) return

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
                is PaginationResult.Success -> {
                    val newPage = result.newPage
                    val embed = paginator.getEmbedPage(newPage)
                    val buttons = paginator.getPaginationButtons(currentPage = newPage, prefix = btnPrefix, key = paginatorKey)

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

    fun getEmbedPage(page: Int): MessageEmbed {
        val validPage = page.coerceIn(0, totalPages - 1)
        val start = validPage * itemsPerPage
        val end = (start + itemsPerPage).coerceAtMost(items.size)

        val embed = embedBuilder(EmbedBuilder().setTitle(title))
        items.subList(start, end).forEachIndexed { i, item -> fieldBuilder(i, item, embed) }

        return embed.build()
    }

    fun getPaginationButtons(
        currentPage: Int,
        prefix: String,
        key: String = "",
        customEmojis: PaginationEmojis = PaginationEmojis()
    ): List<Button> {
        val keyPart = if (key.isNotEmpty()) ":$key" else ""
        val buttons = mutableListOf<Button>()

        if (totalPages >= ADDITIONAL_BUTTONS_MIN_PAGE) {
            buttons.add(
                Button.secondary(
                    "${prefix}${keyPart}:first:$currentPage",
                    customEmojis.first
                ).withDisabled(currentPage == 0)
            )
        }

        buttons.add(
            Button.primary(
                "${prefix}${keyPart}:prev:$currentPage",
                customEmojis.previous
            ).withDisabled(currentPage == 0)
        )

        buttons.add(
            Button.secondary(
                "${prefix}${keyPart}:info:$currentPage",
                "${currentPage + 1}/$totalPages"
            ).withDisabled(true)
        )

        buttons.add(
            Button.primary(
                "${prefix}${keyPart}:next:$currentPage",
                customEmojis.next
            ).withDisabled(currentPage >= totalPages - 1)
        )

        if (totalPages >= ADDITIONAL_BUTTONS_MIN_PAGE) {
            buttons.add(
                Button.secondary(
                    "${prefix}${keyPart}:last:$currentPage",
                    customEmojis.last
                ).withDisabled(currentPage >= totalPages - 1)
            )
        }

        return buttons
    }

    fun handlePaginationAction(
        action: String,
        currentPage: Int,
        event: ButtonInteractionEvent
    ): PaginationResult {
        return when (action) {
            "prev" -> {
                val newPage = (currentPage - 1).coerceIn(0, totalPages - 1)
                PaginationResult.Success(newPage)
            }

            "next" -> {
                val newPage = (currentPage + 1).coerceIn(0, totalPages - 1)
                PaginationResult.Success(newPage)
            }

            "first" -> {
                PaginationResult.Success(0)
            }

            "last" -> {
                PaginationResult.Success(totalPages - 1)
            }

            "info" -> {
                event.deferEdit().queue() // Acknowledge
                PaginationResult.NoAction
            }

            else -> {
                event.reply("❌ Unknown pagination action.").setEphemeral(true).queue()
                PaginationResult.Error
            }
        }
    }
}