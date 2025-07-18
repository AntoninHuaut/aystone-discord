package fr.maner.aystonediscord.command

import fr.maner.aystonediscord.command.helper.CommandPermission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

abstract class AbstractCommand(
    val name: String, val description: String, val options: List<Option> = emptyList(),
    val contextMenuName: String?,
    val rolesId: List<String>
) : ListenerAdapter() {

    data class Option(val name: String, val description: String, val type: OptionType)

    fun createCommands(): List<CommandData> {
        val slashCommand = options.fold(
            extendSlashCommand(Commands.slash(name, description).setContexts(InteractionContextType.GUILD))
        ) { cmd, option ->
            cmd.addOption(option.type, option.name, option.description, false)
        }

        val contextCommand = contextMenuName?.let {
            Commands.user(it).setContexts(InteractionContextType.GUILD)
        }

        return buildList {
            add(slashCommand)
            contextCommand?.let { add(it) }
        }
    }

    open fun extendSlashCommand(slashCommand: SlashCommandData): SlashCommandData {
        return slashCommand
    }

    override fun onUserContextInteraction(event: UserContextInteractionEvent) {
        if (contextMenuName != null && event.name != contextMenuName) return
        if (!CommandPermission.hasPermission(event, event.member, rolesId)) return

        onUserContextInteractionAfterPermission(event)
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != name) return
        if (!CommandPermission.hasPermission(event, event.member, rolesId)) return

        onSlashCommandInteractionAfterPermission(event)
    }

    abstract fun onSlashCommandInteractionAfterPermission(event: SlashCommandInteractionEvent)
    abstract fun onUserContextInteractionAfterPermission(event: UserContextInteractionEvent)
}