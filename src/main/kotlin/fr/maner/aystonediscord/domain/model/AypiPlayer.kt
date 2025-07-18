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
                discordId = identities.discord.userId,
                discordName = identities.discord.userName,
                mcUuid = identities.microsoft.userId,
                mcName = identities.microsoft.userName,
                twitchId = identities.twitch.userId,
                twitchName = identities.twitch.userName,
            )
        }
    }
}
