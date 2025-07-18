package fr.maner.aystonediscord.api

import com.google.gson.Gson
import fr.maner.aystonediscord.domain.AystoneConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

class AystoneAPI(private val ayConfig: AystoneConfig) {

    data class UserIdentitiesResponse(
        val discord: UserIdentityResponse<String>,
        val microsoft: UserIdentityResponse<UUID>,
        val twitch: UserIdentityResponse<String>,
    )

    data class UserIdentityResponse<T>(
        val userId: T,
        val userName: String,
    )

    companion object {
        private val logger = KotlinLogging.logger {}
        private val gson = Gson()
    }

    fun getUserByIdpId(idpSource: String, idpUserId: String): UserIdentitiesResponse? {
        val encodedIdpSource = URLEncoder.encode(idpSource, StandardCharsets.UTF_8)
        val encodedUserId = URLEncoder.encode(idpUserId, StandardCharsets.UTF_8)
        val params = "idpSource=$encodedIdpSource&userId=$encodedUserId"
        val url = "${ayConfig.url}/user?$params"

        try {
            return gson.fromJson(HttpAPI.get(url, ayConfig.token), UserIdentitiesResponse::class.java)
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch user ID for idpAlias '$idpSource' and idpUserId '$idpUserId' from Aypi." }
            return null
        }
    }
}
