package fr.maner.aystonediscord.repository

import fr.maner.aystonediscord.domain.model.AystoneInstance
import fr.maner.aystonediscord.domain.model.AystoneInstanceEntity
import org.jetbrains.exposed.sql.transactions.transaction

class AystoneInstanceRepository {

    fun getAll(): List<AystoneInstance> = transaction {
        AystoneInstanceEntity.all().map { AystoneInstance.fromEntity(it) }
    }
}