package fr.maner.aystonediscord.domain.model

import fr.maner.aystonediscord.api.KeycloakAPI
import java.util.*

data class KeycloakPlayer(
    val discordId: String,
    val discordName: String,
    val mcUuid: UUID,
    val mcName: String,
    val twitchId: String,
    val twitchName: String
) {

    companion object {
        fun from(identities: KeycloakAPI.UserFederatedIdentity): KeycloakPlayer {
            return KeycloakPlayer(
                discordId = identities.discord.userId ?: "",
                discordName = identities.discord.userName ?: "",
                mcUuid = UUID.fromString("b5238882-0706-49c2-992d-538ab1b057f6"), // TODO
                mcName = "Maner_", // TODO
                twitchId = identities.twitch.userId ?: "",
                twitchName = identities.twitch.userName ?: "",
            )
        }
    }
}
