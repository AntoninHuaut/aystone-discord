package fr.maner.aystonediscord.command.helper

import fr.maner.aystonediscord.domain.model.AypiPlayer
import fr.maner.aystonediscord.domain.model.AystoneInstance
import fr.maner.aystonediscord.domain.model.AystonePlayer
import fr.maner.aystonediscord.domain.model.AystoneSanction
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

object EmbedBuilder {

    private const val MINOTAR_URL = "https://minotar.net/avatar"
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH)
    private val EMBED_COLOR = Color(0x1ABC9C)

    fun buildPlayerInfoEmbed(aPlayer: AystonePlayer, kPlayer: AypiPlayer, mcName: String): MessageEmbed {
        return EmbedBuilder()
            .setColor(EMBED_COLOR)
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
            .addField("\u200B", "\u200B", true) // Empty field for spacing
            .addField("Twitch", "`${kPlayer.twitchName}` (`${kPlayer.twitchId}`)", true)
            .addField("Discord", "`${kPlayer.discordName}` (`${kPlayer.discordId}`)", true)
            .build()
    }

    fun buildSanctionField(sanction: AystoneSanction, embed: EmbedBuilder): EmbedBuilder {
        return embed.addField(
            sanction.type.name,
            listOf(
                "*${sanction.sanctionApplied.format(DATE_FORMATTER)}*",
                "${sanction.reason}",
            ).joinToString("\n"),
            true
        )
    }

    fun buildInstanceField(instance: AystoneInstance, embed: EmbedBuilder): EmbedBuilder {
        return embed.addField(
            instance.name,
            listOf(
                "${instance.numberRegisteredPlayers}/${instance.maxPlayer} players",
                if (instance.enabled) "✅ Enabled" else "❌ Disabled",
                if (instance.visible) "✅ Visible" else "❌ Invisible",
            ).joinToString("\n"),
            true
        )
    }
}