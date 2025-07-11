package fr.maner.aystonediscord.domain

enum class Identities(private val type: String) {
    DISCORD("discord"),
    MINECRAFT("minecraft"),
    TWITCH("twitch");

    fun getIdpAlias(identities: Map<String, String>): String? {
        return identities[type]
    }

    fun getType(): String {
        return type
    }
}