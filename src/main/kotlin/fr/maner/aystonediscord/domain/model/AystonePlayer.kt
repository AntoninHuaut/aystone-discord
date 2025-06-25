package fr.maner.aystonediscord.domain.model

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.*

object AystonePlayersTable : IdTable<UUID>("aystone_players") {
    override val id: Column<EntityID<UUID>> = uuid("uuid").entityId()
    val instance = reference(
        "instance",
        AystoneInstancesTable,
        onDelete = ReferenceOption.SET_NULL,
        onUpdate = ReferenceOption.CASCADE
    ).nullable()
    val whitelist = bool("whitelist").default(false)
    val ban = bool("ban").default(false)
    val lastLogin = datetime("last_login").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, instance)
    }
}

class AystonePlayerEntity(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, AystonePlayerEntity>(AystonePlayersTable)

    var uuid by AystonePlayersTable.id
    var instance by AystoneInstanceEntity optionalReferencedOn AystonePlayersTable.instance
    var whitelist by AystonePlayersTable.whitelist
    var ban by AystonePlayersTable.ban
    var lastLogin by AystonePlayersTable.lastLogin
}

data class AystonePlayer(
    val uuid: UUID,
    val instanceName: String?,
    val whitelist: Boolean = false,
    val ban: Boolean = false,
    val lastLogin: LocalDateTime? = null
) {
    companion object {
        fun fromEntity(entity: AystonePlayerEntity): AystonePlayer {
            return AystonePlayer(
                uuid = entity.uuid.value,
                instanceName = entity.instance?.name?.value,
                whitelist = entity.whitelist,
                ban = entity.ban,
                lastLogin = entity.lastLogin,
            )
        }
    }
}