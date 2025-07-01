package fr.maner.aystonediscord.api

import com.google.gson.Gson
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

object MojangAPI {

    private const val UUID_URL = "https://api.mojang.com/users/profiles/minecraft"
    private const val TIMEOUT = 5000
    private val gson = Gson()

    private data class MojangResponse(val name: String, val id: String)

    @Throws(IOException::class)
    fun getUUID(username: String): UUID? {
        val encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8)
        val apiUrl = URI.create("$UUID_URL/$encodedUsername").toURL()
        (apiUrl.openConnection() as HttpURLConnection).run {
            requestMethod = "GET"
            connectTimeout = TIMEOUT
            readTimeout = TIMEOUT

            return try {
                if (responseCode != HttpURLConnection.HTTP_OK) return null

                inputStream.bufferedReader().use { reader ->
                    gson.fromJson(reader, MojangResponse::class.java)?.id?.let { formatUUID(it) }
                }
            } finally {
                disconnect()
            }
        }
    }

    private fun formatUUID(compactUUID: String): UUID {
        return UUID.fromString(
            compactUUID.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})".toRegex(),
                "$1-$2-$3-$4-$5"
            )
        )
    }
}
