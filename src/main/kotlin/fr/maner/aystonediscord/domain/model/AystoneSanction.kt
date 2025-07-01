package fr.maner.aystonediscord.domain.model

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import java.util.*

enum class SanctionType {
    BAN, KICK, WARN, NOTE
}

object AystoneSanctionsTable : IdTable<Int>("aystone_sanctions") {
    override val id: Column<EntityID<Int>> = integer("sanction_id").entityId()
    val playerUuid = reference(
        "player_uuid",
        AystonePlayersTable,
        onDelete = ReferenceOption.SET_NULL,
        onUpdate = ReferenceOption.CASCADE
    )

    // ENUM BAN,KICK,WARN,NOTE
    val type = enumeration("type", SanctionType::class)
    val reason = varchar("reason", 255)

    override val primaryKey = PrimaryKey(id)
}

class AystoneSanctionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AystoneSanctionEntity>(AystoneSanctionsTable)

    var sanctionId by AystoneSanctionsTable.id
    var playerUuid by AystonePlayerEntity referencedOn AystoneSanctionsTable.playerUuid
    var type by AystoneSanctionsTable.type
    var reason by AystoneSanctionsTable.reason
}

data class AystoneSanction(
    val sanctionId: Int,
    val playerUuid: UUID,
    val type: SanctionType,
    val reason: String
) {
    companion object {
        fun fromEntity(entity: AystoneSanctionEntity): AystoneSanction {
            return AystoneSanction(
                sanctionId = entity.sanctionId.value,
                playerUuid = entity.playerUuid.uuid.value,
                type = entity.type,
                reason = entity.reason
            )
        }
    }
}