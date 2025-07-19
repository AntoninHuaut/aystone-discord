package fr.maner.aystonediscord.domain.model

import fr.maner.aystonediscord.api.AystoneAPI
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
        fun from(identities: AystoneAPI.UserIdentitiesResponse): AypiPlayer {
            return AypiPlayer(
                discordId = identities.discord.id,
                discordName = identities.discord.username,
                mcUuid = identities.microsoft.id,
                mcName = identities.microsoft.username,
                twitchId = identities.twitch.id,
                twitchName = identities.twitch.username,
            )
        }
    }
}
