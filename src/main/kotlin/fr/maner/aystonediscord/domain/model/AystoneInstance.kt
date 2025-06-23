package fr.maner.aystonediscord.domain.model

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object AystoneInstancesTable : IdTable<String>("aystone_instances") {
    override val id: Column<EntityID<String>> = varchar("name", 255).entityId()
    val visible = bool("visible")

    override val primaryKey = PrimaryKey(id)
}

class AystoneInstanceEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, AystoneInstanceEntity>(AystoneInstancesTable)

    var name by AystoneInstancesTable.id
    var visible by AystoneInstancesTable.visible
}

data class AystoneInstance(
    val name: String,
    val visible: Boolean
) {
    companion object {
        fun fromEntity(entity: AystoneInstanceEntity): AystoneInstance {
            return AystoneInstance(
                name = entity.name.value,
                visible = entity.visible
            )
        }
    }
}