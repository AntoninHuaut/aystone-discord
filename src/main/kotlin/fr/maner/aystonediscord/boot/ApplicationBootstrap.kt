package fr.maner.aystonediscord.boot

import fr.maner.aystonediscord.api.KeycloakAPI
import fr.maner.aystonediscord.api.TwitchAPI
import fr.maner.aystonediscord.repository.AystoneInstanceRepository
import fr.maner.aystonediscord.repository.AystonePlayerRepository
import fr.maner.aystonediscord.repository.AystoneSanctionRepository

class ApplicationBootstrap {

    fun start(): Result<Unit> {
        return runCatching {
            val config = ConfigLoader.loadConfig().getOrThrow()

            val dbConnection = DatabaseConnection(config.database)

            val kcClient = KeycloakAPI(config.keycloak)
            val twClient = TwitchAPI(config.twitch)

            val discordClient = DiscordClient(
                config.bot, kcClient, twClient,
                AystoneInstanceRepository(), AystonePlayerRepository(), AystoneSanctionRepository()
            )

            Runtime.getRuntime().addShutdownHook(Thread {
                println("Shutting down...")
                discordClient.close()
                dbConnection.close()
            })
        }
    }
}