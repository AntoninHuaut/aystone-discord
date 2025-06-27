package fr.maner.aystonediscord.repository

import fr.maner.aystonediscord.domain.model.AystoneSanction
import fr.maner.aystonediscord.domain.model.AystoneSanctionEntity
import fr.maner.aystonediscord.domain.model.AystoneSanctionsTable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class AystoneSanctionRepository {

    fun getAll(): List<AystoneSanction> = transaction {
        AystoneSanctionEntity.all().map { AystoneSanction.fromEntity(it) }
    }

    fun getByUuid(id: Int): AystoneSanction? = transaction {
        AystoneSanctionEntity.findById(id)?.let { AystoneSanction.fromEntity(it) }
    }

    fun getByUuid(uuid: UUID): List<AystoneSanction> = transaction {
        AystoneSanctionEntity.find { AystoneSanctionsTable.uuid eq uuid }
            .map { AystoneSanction.fromEntity(it) }
    }
}
