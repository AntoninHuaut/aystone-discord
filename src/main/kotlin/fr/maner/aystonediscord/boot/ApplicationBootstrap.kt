package fr.maner.aystonediscord.boot

import fr.maner.aystonediscord.infrastructure.database.DatabaseConnection

class ApplicationBootstrap {

    fun start(): Result<Unit> {
        return runCatching {
            val config = ConfigLoader.loadConfig().getOrThrow()

            val dbConnection = DatabaseConnection(config.database)
            dbConnection.connect()

            val discordClient = DiscordClient(config.bot.token)
            val jda = discordClient.buildBot(config.bot.activity)

            Runtime.getRuntime().addShutdownHook(Thread {
                println("Shutting down...")
                jda.shutdown()
                dbConnection.close()
            })

            println("Application started successfully!")
        }
    }
}