package fr.maner.aystonediscord.domain.model

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object AystoneInstancesTable : IdTable<String>("aystone_instances") {
    override val id: Column<EntityID<String>> = varchar("name", 255).entityId()
    val visible = bool("visible")
    val maxPlayer = integer("max_player")

    override val primaryKey = PrimaryKey(id)
}

class AystoneInstanceEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, AystoneInstanceEntity>(AystoneInstancesTable)

    val name by AystoneInstancesTable.id
    val visible by AystoneInstancesTable.visible
    val maxPlayer by AystoneInstancesTable.maxPlayer
}

data class AystoneInstance(
    val name: String,
    val visible: Boolean,
    val maxPlayer: Int
) {
    companion object {
        fun fromEntity(entity: AystoneInstanceEntity): AystoneInstance {
            return AystoneInstance(
                name = entity.name.value,
                visible = entity.visible,
                maxPlayer = entity.maxPlayer
            )
        }
    }
}