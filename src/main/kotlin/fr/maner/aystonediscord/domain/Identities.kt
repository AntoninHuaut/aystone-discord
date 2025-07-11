package fr.maner.aystonediscord.domain

enum class Identities(private val idpAlias: String) {
    DISCORD("discord"),
    TWITCH("twitch");

    fun getIdpAlias(): String {
        return idpAlias
    }
}