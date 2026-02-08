package bot

import (
	"log/slog"

	"github.com/bwmarrin/discordgo"
)

const (
	instanceButtonPrefix = "instance_list"
	itemsPerPage         = 9
)

func (b *Bot) handleInstance(s *discordgo.Session, i *discordgo.InteractionCreate) {
	if !CheckPermission(s, i, b.config.RolesID) {
		return
	}

	data := i.ApplicationCommandData()
	if len(data.Options) == 0 {
		RespondError(s, i, "❌ Please provide a subcommand.")
		return
	}

	subcommand := data.Options[0].Name

	switch subcommand {
	case "list":
		b.handleInstanceList(s, i)
	default:
		RespondError(s, i, "❌ Unknown subcommand.")
	}
}

func (b *Bot) handleInstanceList(s *discordgo.Session, i *discordgo.InteractionCreate) {
	if i.Member == nil {
		RespondError(s, i, "❌ This command can only be used in a server.")
		return
	}

	instances, err := b.instanceRepo.GetAll(b.ctx)
	if err != nil {
		slog.Error("Failed to fetch instances", "error", err)
		RespondError(s, i, "❌ Failed to fetch instances.")
		return
	}

	if len(instances) == 0 {
		RespondError(s, i, "❌ No instances found.")
		return
	}

	buildPage := func(page int) *discordgo.MessageEmbed {
		start := page * itemsPerPage
		end := min(start+itemsPerPage, len(instances))

		embed := &discordgo.MessageEmbed{
			Title:  "📋 Instance List",
			Color:  embedColor,
			Fields: []*discordgo.MessageEmbedField{},
		}

		for _, instance := range instances[start:end] {
			BuildInstanceField(instance, embed)
		}

		return embed
	}

	embed, components := CreatePagination(i.Member.User.ID, i.ID, instanceButtonPrefix, len(instances), itemsPerPage, buildPage)

	err = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
		Type: discordgo.InteractionResponseChannelMessageWithSource,
		Data: &discordgo.InteractionResponseData{
			Embeds:     []*discordgo.MessageEmbed{embed},
			Components: components,
		},
	})
	if err != nil {
		slog.Warn("Failed to send instance list response", "error", err)
	}
}
