package fr.maner.aystonediscord.command.helper

import fr.maner.aystonediscord.api.PlayerDBApi
import fr.maner.aystonediscord.repository.AystoneSanctionRepository
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.util.*

class SanctionButtonHandler(
    private val aystoneSanctionRepository: AystoneSanctionRepository,
    private val rolesId: List<String>
) {

    companion object {
        private const val BUTTON_PREFIX_SANCTION_NOTHING = "whois_sanction_nothing"
        private const val BUTTON_PREFIX_SANCTION_ASK = "whois_sanction_ask"
        private const val BUTTON_PREFIX_SANCTION_SEE = "whois_sanction_see"
        private const val MINOTAR_URL = "https://minotar.net/avatar"
    }

    fun createSanctionButtons(uuid: UUID): List<Button> {
        val buttons = mutableListOf<Button>()
        val nbSanctions = aystoneSanctionRepository.countByUuid(uuid)

        if (nbSanctions > 0) {
            buttons.add(
                Button.primary(
                    "${BUTTON_PREFIX_SANCTION_ASK}:${uuid}",
                    if (nbSanctions > 1) "See the $nbSanctions sanctions" else "See the sanction"
                )
                    .withEmoji(Emoji.fromUnicode("⚠"))
            )
        } else {
            buttons.add(
                Button.secondary("${BUTTON_PREFIX_SANCTION_NOTHING}:${uuid}", "No sanction")
                    .withEmoji(Emoji.fromUnicode("✅"))
                    .asDisabled()
            )
        }

        return buttons
    }

    fun handleButtonInteraction(event: ButtonInteractionEvent): Boolean {
        val componentId = event.componentId

        return when {
            componentId.startsWith(BUTTON_PREFIX_SANCTION_SEE) -> {
                PaginatedEmbed.handleUpdatePaginationAndPermission(event, BUTTON_PREFIX_SANCTION_SEE, rolesId)
                true
            }

            componentId.startsWith(BUTTON_PREFIX_SANCTION_ASK) -> {
                handleSanctionAskButton(event)
                true
            }

            else -> false
        }
    }

    private fun handleSanctionAskButton(event: ButtonInteractionEvent) {
        if (!CommandPermission.hasPermission(event, event.member, rolesId)) {
            return
        }

        val uuidRaw = event.componentId.substringAfter("$BUTTON_PREFIX_SANCTION_ASK:")
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

        val mcInfo = PlayerDBApi.getByNameOrUuid(uuid.toString()) ?: run {
            event.reply("❌ Error while fetching Minecraft name for `$uuid`.").setEphemeral(true).queue()
            return
        }

        val (embed, buttons) = PaginatedEmbed.handleNewPagination(
            event, BUTTON_PREFIX_SANCTION_SEE, sanctions,
            title = "⚖\uFE0F Sanction List: `${mcInfo.username}`",
            itemsPerPage = 9,
            fieldBuilder = { _, sanction, embed -> EmbedBuilder.buildSanctionField(sanction, embed) },
            embedBuilder = {
                it.setThumbnail("$MINOTAR_URL/${uuid}.png")
                it.setFooter(
                    "Requested by ${event.user.name}",
                    event.user.effectiveAvatarUrl
                )
            }
        )

        event.replyEmbeds(embed).setActionRow(*buttons.map { it as ItemComponent }.toTypedArray()).queue()
    }
}