package fr.maner.aystonediscord.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object PlayerDBApi {

    private val logger = KotlinLogging.logger {}
    private val gson = Gson()

    private const val MINECRAFT_URL = "https://playerdb.co/api/player/minecraft/"

    private data class PlayerDBResponse(
        val code: String,
        val message: String,
        val data: PlayerData,
        val success: Boolean
    )

    private data class PlayerData(
        val player: Player
    )

    private data class Player(
        val username: String,
        val id: String,
        @SerializedName("raw_id")
        val rawId: String
    )

    data class PlayerInfo(val username: String, val id: String, val rawId: String)

    fun getByNameOrUuid(param: String): PlayerInfo? {
        val encodedParam = URLEncoder.encode(param, StandardCharsets.UTF_8)
        val url = "$MINECRAFT_URL$encodedParam"

        try {
            return gson.fromJson(HttpAPI.get(url), PlayerDBResponse::class.java)?.let { response ->
                PlayerInfo(response.data.player.username, response.data.player.id, response.data.player.rawId)
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch player info for '$param' from PlayerDB API." }
            return null
        }
    }
}
