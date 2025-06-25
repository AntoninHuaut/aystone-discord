package fr.maner.aystonediscord.infrastructure.repository

import fr.maner.aystonediscord.domain.model.AystonePlayer
import fr.maner.aystonediscord.domain.model.AystonePlayerEntity
import fr.maner.aystonediscord.domain.model.AystonePlayersTable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class AystonePlayerRepository {

    fun getAll(): List<AystonePlayer> = transaction {
        AystonePlayerEntity.all().map { AystonePlayer.fromEntity(it) }
    }

    fun getByUuid(uuid: UUID): AystonePlayer? = transaction {
        AystonePlayerEntity.findById(uuid)?.let { AystonePlayer.fromEntity(it) }
    }

    fun getByInstance(instanceName: String): List<AystonePlayer> = transaction {
        AystonePlayerEntity.find { AystonePlayersTable.instance eq instanceName }
            .map { AystonePlayer.fromEntity(it) }
    }
}
