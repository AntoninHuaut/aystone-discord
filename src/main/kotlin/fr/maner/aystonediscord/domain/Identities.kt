package fr.maner.aystonediscord.domain

enum class Identities(private val type: String) {
    DISCORD("discord"),
    MICROSOFT("microsoft"),
    TWITCH("twitch");

    fun getType(): String {
        return type
    }
}