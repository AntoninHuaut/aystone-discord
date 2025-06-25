package fr.maner.aystonediscord.boot

class ApplicationBootstrap {

    fun start(): Result<Unit> {
        return runCatching {
            val config = ConfigLoader.loadConfig().getOrThrow()

            val dbConnection = DatabaseConnection(config.database)
            val discordClient = DiscordClient(config.bot)

            Runtime.getRuntime().addShutdownHook(Thread {
                println("Shutting down...")
                discordClient.close()
                dbConnection.close()
            })
        }
    }
}