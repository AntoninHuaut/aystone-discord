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
    val whitelist = bool("whitelist").default(false)
    val ban = bool("ban").default(false)
    val instance = reference(
        "instance",
        AystoneInstancesTable,
        onDelete = ReferenceOption.SET_NULL,
        onUpdate = ReferenceOption.CASCADE
    ).nullable()
    val createdOn = datetime("created_on")
    val lastLogin = datetime("last_login")

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, instance)
    }
}

class AystonePlayerEntity(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, AystonePlayerEntity>(AystonePlayersTable)

    var uuid by AystonePlayersTable.id
    var whitelist by AystonePlayersTable.whitelist
    var ban by AystonePlayersTable.ban
    var instance by AystoneInstanceEntity optionalReferencedOn AystonePlayersTable.instance
    val createdOn by AystonePlayersTable.createdOn
    var lastLogin by AystonePlayersTable.lastLogin
}

data class AystonePlayer(
    val uuid: UUID,
    val whitelist: Boolean,
    val ban: Boolean,
    val instanceName: String?,
    val createdOn: LocalDateTime,
    val lastLogin: LocalDateTime
) {
    companion object {
        fun fromEntity(entity: AystonePlayerEntity): AystonePlayer {
            return AystonePlayer(
                uuid = entity.uuid.value,
                whitelist = entity.whitelist,
                ban = entity.ban,
                instanceName = entity.instance?.name?.value,
                createdOn = entity.createdOn,
                lastLogin = entity.lastLogin,
            )
        }
    }
}