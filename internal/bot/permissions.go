package bot

import (
	"github.com/bwmarrin/discordgo"
)

func HasPermission(i *discordgo.InteractionCreate, rolesID []string) bool {
	member := i.Member
	if member == nil {
		return false
	}

	perms := i.Interaction.Member.Permissions
	if perms&discordgo.PermissionAdministrator != 0 {
		return true
	}

	for _, roleID := range rolesID {
		for _, userRoleID := range member.Roles {
			if roleID == userRoleID {
				return true
			}
		}
	}

	return false
}
