package fr.maner.aystonediscord.boot

import fr.maner.aystonediscord.repository.AystoneInstanceRepository
import fr.maner.aystonediscord.repository.AystonePlayerRepository

class ApplicationBootstrap {

    fun start(): Result<Unit> {
        return runCatching {
            val config = ConfigLoader.loadConfig().getOrThrow()

            val dbConnection = DatabaseConnection(config.database)
            val discordClient = DiscordClient(config.bot, AystonePlayerRepository(), AystoneInstanceRepository())

            Runtime.getRuntime().addShutdownHook(Thread {
                println("Shutting down...")
                discordClient.close()
                dbConnection.close()
            })
        }
    }
}