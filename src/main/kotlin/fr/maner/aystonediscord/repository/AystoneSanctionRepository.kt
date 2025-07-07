package fr.maner.aystonediscord.repository

import fr.maner.aystonediscord.domain.model.AystoneSanction
import fr.maner.aystonediscord.domain.model.AystoneSanctionEntity
import fr.maner.aystonediscord.domain.model.AystoneSanctionsTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class AystoneSanctionRepository {

    fun getByUuidSortDateDesc(uuid: UUID): List<AystoneSanction> = transaction {
        AystoneSanctionEntity.find { AystoneSanctionsTable.playerUuid eq uuid }
            .orderBy(AystoneSanctionsTable.sanctionApplied to SortOrder.DESC)
            .map { AystoneSanction.fromEntity(it) }
    }

    fun countByUuid(uuid: UUID): Long = transaction {
        AystoneSanctionEntity.count(AystoneSanctionsTable.playerUuid eq uuid)
    }
}
