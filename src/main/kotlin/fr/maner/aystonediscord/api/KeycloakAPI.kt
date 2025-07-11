package fr.maner.aystonediscord.api

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import fr.maner.aystonediscord.domain.Identities
import fr.maner.aystonediscord.domain.KeycloakConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

class KeycloakAPI(private val kcConfig: KeycloakConfig) {

    data class UserFederatedIdentifyResponse(
        val identityProvider: String,
        val userId: String,
        val userName: String,
    )

    data class UserResponse(val id: String, val username: String, val email: String?)

    private data class AuthResponse(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("expires_in") val accessExpiresIn: Int,
        @Transient val date: Instant,
    )

    data class UserFederatedIdentity(val discord: GenericIdentity, val twitch: GenericIdentity)
    data class GenericIdentity(val userId: String?, val userName: String?)

    companion object {
        private val logger = KotlinLogging.logger {}
        private val gson = Gson()
    }

    private var previousAuthResponse: AuthResponse? = null

    fun getIdentitiesMap(): Map<String, String> {
        return kcConfig.identities
    }

    fun getFederatedIdentitiesByIdpId(idpAlias: String, idpUserId: String): UserFederatedIdentity? {
        val userId = getUserIdpByIdpId(idpAlias, idpUserId) ?: return null
        return getFederatedIdentitiesByUserId(userId)
    }

    private fun getUserIdpByIdpId(idpAlias: String, idpUserId: String): String? {
        val encodedIdpAlias = URLEncoder.encode(idpAlias, StandardCharsets.UTF_8)
        val encodedUserId = URLEncoder.encode(idpUserId, StandardCharsets.UTF_8)
        val params = "idpAlias=$encodedIdpAlias&idpUserId=$encodedUserId"
        val url = "${kcConfig.url}/admin/realms/${kcConfig.realm}/users?$params"

        val type = object : TypeToken<List<UserResponse>>() {}.type

        try {
            val content = getAuth(url) ?: return null
            val users = gson.fromJson<List<UserResponse>>(content, type) ?: return null

            if (users.isEmpty()) {
                return null
            }

            return users.firstOrNull()?.id
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch user ID for idpAlias '$idpAlias' and idpUserId '$idpUserId' from Keycloak API." }
            return null
        }
    }

    private fun getFederatedIdentitiesByUserId(userId: String): UserFederatedIdentity? {
        val encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8)
        val url = "${kcConfig.url}/admin/realms/${kcConfig.realm}/users/$encodedUserId/federated-identity"

        val type = object : TypeToken<List<UserFederatedIdentifyResponse>>() {}.type

        try {
            val content = getAuth(url) ?: return null
            val identities = gson.fromJson<List<UserFederatedIdentifyResponse>>(content, type) ?: return null

            val discordIdentity = Identities.DISCORD.getIdpAlias(kcConfig.identities) ?: return null
            val twitchIdentity = Identities.TWITCH.getIdpAlias(kcConfig.identities) ?: return null

            val discord = identities.find { it.identityProvider == discordIdentity }
            val twitch = identities.find { it.identityProvider == twitchIdentity }

            return UserFederatedIdentity(
                discord = GenericIdentity(discord?.userId, discord?.userName),
                twitch = GenericIdentity(twitch?.userId, twitch?.userName),
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to fetch federated identities for user ID '$userId' from Keycloak API." }
            return null
        }
    }

    private fun auth(): AuthResponse {
        val url = "${kcConfig.url}/realms/${kcConfig.realm}/protocol/openid-connect/token"
        val params = mapOf(
            "client_id" to kcConfig.clientId,
            "grant_type" to "password",
            "username" to kcConfig.username,
            "password" to kcConfig.password,
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
                logger.error(e) { "Failed to authenticate with Keycloak API." }
                return null
            }
        }

        return HttpAPI.get(url, authResponse?.accessToken)
    }
}
