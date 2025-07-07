package fr.maner.aystonediscord.api

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object PlayerDBApi {

    private const val MINECRAFT_URL = "https://playerdb.co/api/player/minecraft/"
    private const val TIMEOUT = 5000
    private val gson = Gson()

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
        val raw_id: String
    )

    data class PlayerInfo(val username: String, val id: String, val rawId: String)

    suspend fun getByNameOrUuid(param: String): PlayerInfo? = withContext(Dispatchers.IO) {
        val encodedParam = URLEncoder.encode(param, StandardCharsets.UTF_8)
        val url = "$MINECRAFT_URL$encodedParam"

        makeApiRequest<PlayerInfo>(url) { response ->
            with(response.data.player) {
                PlayerInfo(username, id, raw_id)
            }
        }
    }

    private inline fun <T> makeApiRequest(
        url: String,
        transform: (PlayerDBResponse) -> T?
    ): T? {
        val connection = URI.create(url).toURL().openConnection() as HttpURLConnection

        connection.apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT
            readTimeout = TIMEOUT
        }

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            return null
        }

        return connection.inputStream.bufferedReader().use { reader ->
            gson.fromJson(reader, PlayerDBResponse::class.java)?.let(transform)
        }
    }
}
