package fr.maner.aystonediscord.domain.model

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.*

enum class SanctionType {
    BAN, UNBAN, KICK, WARN, NOTE
}

object AystoneSanctionsTable : IdTable<Int>("aystone_sanctions") {
    override val id: Column<EntityID<Int>> = integer("sanction_id").entityId()
    val playerUuid = reference(
        "player_uuid",
        AystonePlayersTable,
        onDelete = ReferenceOption.SET_NULL,
        onUpdate = ReferenceOption.CASCADE
    )

    val type = customEnumeration("type", "VARCHAR", { value -> SanctionType.valueOf(value as String) }, { it.name })
    val reason = varchar("reason", 255).nullable()
    val until = datetime("until").nullable()
    val sanctionApplied = datetime("sanction_applied").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}

class AystoneSanctionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AystoneSanctionEntity>(AystoneSanctionsTable)

    val sanctionId by AystoneSanctionsTable.id
    val playerUuid by AystonePlayerEntity referencedOn AystoneSanctionsTable.playerUuid
    val type by AystoneSanctionsTable.type
    val reason by AystoneSanctionsTable.reason
    val until by AystoneSanctionsTable.until
    val sanctionApplied by AystoneSanctionsTable.sanctionApplied
}

data class AystoneSanction(
    val sanctionId: Int,
    val playerUuid: UUID,
    val type: SanctionType,
    val reason: String?,
    val until: LocalDateTime?,
    val sanctionApplied: LocalDateTime
) {
    companion object {
        fun fromEntity(entity: AystoneSanctionEntity): AystoneSanction {
            return AystoneSanction(
                sanctionId = entity.sanctionId.value,
                playerUuid = entity.playerUuid.uuid.value,
                type = entity.type,
                reason = entity.reason,
                until = entity.until,
                sanctionApplied = entity.sanctionApplied
            )
        }
    }
}