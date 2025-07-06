package fr.maner.aystonediscord.domain.model

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object AystoneInstancesTable : IdTable<String>("aystone_instances") {
    override val id: Column<EntityID<String>> = varchar("name", 255).entityId()
    val visible = bool("visible").default(false)
    val maxPlayer = integer("max_player")
    val ipAddress = varchar("ip_address", 15).default("0.0.0.0")
    val port = integer("port").default(25565)
    val enabled = bool("enabled").default(false)
    val cnfApplied = bool("cnfapplied").default(false)

    override val primaryKey = PrimaryKey(id)
}

class AystoneInstanceEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, AystoneInstanceEntity>(AystoneInstancesTable)

    val name by AystoneInstancesTable.id
    val visible by AystoneInstancesTable.visible
    val maxPlayer by AystoneInstancesTable.maxPlayer
    val ipAddress by AystoneInstancesTable.ipAddress
    val port by AystoneInstancesTable.port
    val enabled by AystoneInstancesTable.enabled
    val cnfApplied by AystoneInstancesTable.cnfApplied
}

data class AystoneInstance(
    val name: String,
    val visible: Boolean,
    val maxPlayer: Int,
    val ipAddress: String,
    val port: Int,
    val enabled: Boolean,
    val cnfApplied: Boolean
) {
    companion object {
        fun fromEntity(entity: AystoneInstanceEntity): AystoneInstance {
            return AystoneInstance(
                name = entity.name.value,
                visible = entity.visible,
                maxPlayer = entity.maxPlayer,
                ipAddress = entity.ipAddress,
                port = entity.port,
                enabled = entity.enabled,
                cnfApplied = entity.cnfApplied
            )
        }
    }
}