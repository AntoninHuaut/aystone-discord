package fr.maner.aystonediscord.command.helper

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

object CommandPermission {

    fun hasPermission(replyCallback: IReplyCallback, member: Member?, rolesId: List<String>): Boolean {
        if (member != null && (member.hasPermission(Permission.ADMINISTRATOR) || rolesId.any { member.roles.any { role -> role.id == it } })) {
            return true
        }

        replyCallback.reply("❌ You do not have permission to use this command.").setEphemeral(true).queue()
        return false
    }
}