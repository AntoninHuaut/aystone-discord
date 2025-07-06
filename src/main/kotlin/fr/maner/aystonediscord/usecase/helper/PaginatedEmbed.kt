package fr.maner.aystonediscord.usecase.helper

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button

class PaginatedEmbed<T>(
    private val items: List<T>,
    private val title: String = "Paginated List",
    private val itemsPerPage: Int = 5,
    private val fieldBuilder: (Int, T, EmbedBuilder) -> EmbedBuilder
) {
    private val totalPages = (items.size + itemsPerPage - 1) / itemsPerPage

    companion object {
        const val ADDITIONAL_BUTTONS_MIN_PAGE = 3
    }

    fun getEmbedPage(page: Int): MessageEmbed {
        val validPage = page.coerceIn(0, totalPages - 1)
        val start = validPage * itemsPerPage
        val end = (start + itemsPerPage).coerceAtMost(items.size)

        val embed = EmbedBuilder().setTitle(title)
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
}