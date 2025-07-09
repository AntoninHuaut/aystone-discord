package fr.maner.aystonediscord.domain

data class AppConfig(
    var bot: BotConfig = BotConfig(""),
    var database: DatabaseConfig = DatabaseConfig(""),
)

data class BotConfig(
    var token: String = "",
    var activity: String = "",
    var rolesId: List<String> = emptyList(),
)

data class DatabaseConfig(
    var host: String = "",
    var port: String = "",
    var name: String = "",
    var schema: String = "",
    var username: String = "",
    var password: String = ""
)