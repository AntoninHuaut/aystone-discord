package fr.maner.aystonediscord.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import fr.maner.aystonediscord.domain.TwitchConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

class TwitchAPI(private val twConfig: TwitchConfig) {

    data class UsersResponse(
        val data: List<UsersDataResponse>,
    )

    data class UsersDataResponse(
        val id: String,
        val login: String,
    )

    private data class AuthResponse(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("expires_in") val accessExpiresIn: Int,
        @Transient val date: Instant,
    )

    private var previousAuthResponse: AuthResponse? = null

    companion object {
        private val logger = KotlinLogging.logger {}
        private val gson = Gson()
    }

    fun getUserId(username: String): String? {
        val encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8)
        val params = "login=$encodedUsername"
        val url = "${twConfig.apiUrl}/users?$params"

        try {
            val content = getAuth(url)
            val users = gson.fromJson(content, UsersResponse::class.java) ?: return null

            if (users.data.isEmpty()) {
                return null
            }

            return users.data.firstOrNull()?.id
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch user ID for username '$username' from Twitch API." }
            return null
        }
    }

    private fun auth(): AuthResponse {
        val url = twConfig.tokenUrl
        val params = mapOf(
            "client_id" to twConfig.clientId,
            "client_secret" to twConfig.clientSecret,
            "grant_type" to "client_credentials",
        )

        val content = HttpAPI.postFormData(url, params)
        return gson.fromJson(content, AuthResponse::class.java).copy(date = Instant.now())
    }

    private fun getAuth(url: String): String? {
        val authResponse = run {
            try {
                if (previousAuthResponse == null || Instant.now()
                        .isAfter(previousAuthResponse!!.date.plusSeconds(previousAuthResponse!!.accessExpiresIn.toLong()))
                ) {
                    previousAuthResponse = auth()
                    previousAuthResponse
                } else {
                    previousAuthResponse
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to authenticate with Twitch API." }
                return null
            }
        }

        return HttpAPI.get(
            url,
            mapOf("Client-ID" to twConfig.clientId, "Authorization" to "Bearer ${authResponse?.accessToken}")
        )
    }
}
