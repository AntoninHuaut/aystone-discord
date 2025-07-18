package fr.maner.aystonediscord.domain

data class AppConfig(
    var bot: BotConfig = BotConfig(),
    var aystone: AystoneConfig = AystoneConfig(),
    var twitch: TwitchConfig = TwitchConfig(),
    var database: DatabaseConfig = DatabaseConfig(),
)

data class BotConfig(
    var token: String = "",
    var activity: String = "",
    var rolesId: List<String> = emptyList(),
)

data class AystoneConfig(
    var url: String = "",
    var token: String = "",
)

data class TwitchConfig(
    var apiUrl: String = "",
    var tokenUrl: String = "",
    var clientId: String = "",
    var clientSecret: String = "",
)

data class DatabaseConfig(
    var host: String = "",
    var port: String = "",
    var name: String = "",
    var schema: String = "",
    var username: String = "",
    var password: String = "",
)