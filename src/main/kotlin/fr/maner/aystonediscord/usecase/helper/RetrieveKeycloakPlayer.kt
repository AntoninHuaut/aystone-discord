package fr.maner.aystonediscord.usecase.helper

import fr.maner.aystonediscord.api.PlayerDBApi
import fr.maner.aystonediscord.domain.model.KeycloakPlayer
import fr.maner.aystonediscord.usecase.WhoisCommand.Options
import kotlinx.coroutines.runBlocking
import java.util.*

class KeycloakMessageException(
    message: String,
) : Exception(message)

object KeycloakPlayerRetrieve {

    fun byOption(optionName: String, optionValue: String): KeycloakPlayer? {
        val kPlayer = when (optionName) {
            Options.TWITCH_ID.name -> {
                val twitchId = optionValue
                // TODO get KeycloakPlayer by ID Twitch
                KeycloakPlayer("", UUID.randomUUID(), twitchId)
            }

            Options.TWITCH_NAME.name -> {
                val twitchName = optionValue
                // TODO get Twitch ID by name
                // TODO get KeycloakPlayer by ID Twitch
                KeycloakPlayer("", UUID.randomUUID(), "")
            }

            Options.MC_UUID.name -> {
                val mcUuidRaw = optionValue
                val mcUuid: UUID = try {
                    UUID.fromString(mcUuidRaw) ?: throw IllegalArgumentException()
                } catch (_: IllegalArgumentException) {
                    throw KeycloakMessageException("Invalid UUID format: `$mcUuidRaw`")
                }

                // TODO get KeycloakPlayer by UUID Minecraft
                KeycloakPlayer("", mcUuid, "")
            }

            Options.MC_NAME.name -> {
                val mcName = optionValue
                val mcUUID: UUID = runBlocking {
                    try {
                        val mcInfo = PlayerDBApi.getByNameOrUuid(mcName) ?: throw Exception("not found")
                        UUID.fromString(mcInfo.id)
                    } catch (e: Exception) {
                        throw KeycloakMessageException("❌ Error while fetching Minecraft UUID for `$mcName`: ${e.message}.")
                    }
                }

                // TODO get KeycloakPlayer by UUID Minecraft
                KeycloakPlayer("", mcUUID, "")
            }

            else -> null
        }

        return kPlayer
    }
}