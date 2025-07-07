package fr.maner.aystonediscord.repository

import fr.maner.aystonediscord.domain.model.AystonePlayer
import fr.maner.aystonediscord.domain.model.AystonePlayerEntity
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class AystonePlayerRepository {

    fun getByUuid(uuid: UUID): AystonePlayer? = transaction {
        AystonePlayerEntity.findById(uuid)?.let { AystonePlayer.fromEntity(it) }
    }
}
