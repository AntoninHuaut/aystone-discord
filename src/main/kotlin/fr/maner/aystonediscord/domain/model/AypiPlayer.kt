package fr.maner.aystonediscord.domain.model

import fr.maner.aystonediscord.api.AystoneAPI
import fr.maner.aystonediscord.api.PlayerDBApi
import net.dv8tion.jda.api.JDA
import java.util.*

data class AypiPlayer(
    val discordId: String,
    val discordName: String,
    val mcUuid: UUID,
    val mcName: String,
    val twitchId: String,
    val twitchName: String
) {

    companion object {
        fun from(jda: JDA, identities: AystoneAPI.UserIdentitiesResponse): AypiPlayer {
            val overrideMcUUid = identities.microsoft.id ?: identities.microsoft.username?.let {
                try {
                    PlayerDBApi.getByNameOrUuid(it)?.id?.let { id -> UUID.fromString(id) }
                } catch (e: Exception) {
                    null
                }
            } ?: UUID.fromString("00000000-0000-0000-0000-000000000000")

            val overrideDiscordName = identities.discord.username ?: identities.discord.id?.let {
                try {
                    jda.retrieveUserById(it).complete().name
                } catch (e: Exception) {
                    null
                }
            } ?: ""

            return AypiPlayer(
                discordId = identities.discord.id ?: "",
                discordName = overrideDiscordName,
                mcUuid = overrideMcUUid,
                mcName = identities.microsoft.username ?: "",
                twitchId = identities.twitch.id ?: "",
                twitchName = identities.twitch.username ?: "",
            )
        }
    }
}
