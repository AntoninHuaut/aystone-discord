package fr.maner.aystonediscord

import fr.maner.aystonediscord.boot.ApplicationBootstrap
import kotlin.system.exitProcess

fun main() {
    ApplicationBootstrap().start()
        .onFailure { error ->
            println("Failed to start application: ${error.message}")
            exitProcess(1)
        }
}