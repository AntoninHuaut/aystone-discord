package fr.maner.aystonediscord.repository

import fr.maner.aystonediscord.domain.model.AystoneInstance
import fr.maner.aystonediscord.domain.model.AystoneInstanceEntity
import fr.maner.aystonediscord.domain.model.AystonePlayersTable
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.transactions.transaction

class AystoneInstanceRepository {

    fun getAll(): List<AystoneInstance> = transaction {
        val playerCount = AystonePlayersTable.id.count()

        val query = AystonePlayersTable
            .select(AystonePlayersTable.instance, playerCount)
            .groupBy(AystonePlayersTable.instance)
            .associate { row ->
                row[AystonePlayersTable.instance]?.value to row[playerCount].toInt()
            }

        AystoneInstanceEntity.all().map { entity ->
            AystoneInstance(
                name = entity.name.value,
                visible = entity.visible,
                maxPlayer = entity.maxPlayer,
                ipAddress = entity.ipAddress,
                port = entity.port,
                enabled = entity.enabled,
                cnfApplied = entity.cnfApplied,
                numberRegisteredPlayers = query[entity.name.value] ?: 0
            )
        }
    }
}