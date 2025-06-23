package fr.maner.aystonediscord.domain.model

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object AystonePlayersTable : IdTable<String>("aystone_players") {
    override val id: Column<EntityID<String>> = varchar("uuid", 255).entityId()
    val instance = reference(
        "instance",
        AystoneInstancesTable,
        onDelete = ReferenceOption.SET_NULL,
        onUpdate = ReferenceOption.CASCADE
    ).nullable()
    val isWhitelist = bool("is_whitelist").default(false)
    val isBan = bool("is_ban").default(false)
    val lastConnection = datetime("last_connection").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, instance)
    }
}

class AystonePlayerEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, AystonePlayerEntity>(AystonePlayersTable)

    var uuid by AystonePlayersTable.id
    var instance by AystoneInstanceEntity optionalReferencedOn AystonePlayersTable.instance
    var isWhitelist by AystonePlayersTable.isWhitelist
    var isBan by AystonePlayersTable.isBan
    var lastConnection by AystonePlayersTable.lastConnection
}

data class AystonePlayer(
    val uuid: String,
    val instanceName: String?,
    val isWhitelist: Boolean = false,
    val isBan: Boolean = false,
    val lastConnection: LocalDateTime? = null
) {
    companion object {
        fun fromEntity(entity: AystonePlayerEntity): AystonePlayer {
            return AystonePlayer(
                uuid = entity.uuid.value,
                instanceName = entity.instance?.name?.value,
                isWhitelist = entity.isWhitelist,
                isBan = entity.isBan,
                lastConnection = entity.lastConnection,
            )
        }
    }
}