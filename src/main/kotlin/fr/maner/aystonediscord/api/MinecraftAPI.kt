package fr.maner.aystonediscord.api

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

object MinecraftAPI {

    private const val BASE_URL = "https://api.minecraftservices.com/minecraft/profile"
    private const val PROFILE_BY_NAME = "lookup/name"
    private const val PROFILE_BY_UUID = "lookup"
    private const val TIMEOUT = 5000
    private val gson = Gson()

    private data class MinecraftProfileResponse(val name: String, val id: String)

    suspend fun getUUID(username: String): UUID? = withContext(Dispatchers.IO) {
        if (username.isBlank()) return@withContext null

        val encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8)
        val url = "$BASE_URL/$PROFILE_BY_NAME/$encodedUsername"

        makeApiRequest<UUID>(url) { response -> formatUUID(response.id) }
    }

    suspend fun getName(uuid: UUID): String? = withContext(Dispatchers.IO) {
        val encodedUuid = URLEncoder.encode(uuid.toString(), StandardCharsets.UTF_8)
        val url = "$BASE_URL/$PROFILE_BY_UUID/$encodedUuid"

        makeApiRequest<String>(url) { response -> response.name }
    }

    private inline fun <T> makeApiRequest(
        url: String,
        transform: (MinecraftProfileResponse) -> T?
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
            gson.fromJson(reader, MinecraftProfileResponse::class.java)?.let(transform)
        }
    }

    private fun formatUUID(uuid: String): UUID {
        return UUID.fromString(
            if (uuid.length == 32)
                StringBuilder(uuid)
                    .insert(8, '-')
                    .insert(13, '-')
                    .insert(18, '-')
                    .insert(23, '-')
                    .toString()
            else uuid
        )
    }
}
