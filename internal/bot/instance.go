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
	if !HasPermission(i, b.config.RolesID) {
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ You do not have permission to use this command.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	data := i.ApplicationCommandData()
	if len(data.Options) == 0 {
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ Please provide a subcommand.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	subcommand := data.Options[0].Name

	switch subcommand {
	case "list":
		b.handleInstanceList(s, i)
	default:
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ Unknown subcommand.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
	}
}

func (b *Bot) handleInstanceList(s *discordgo.Session, i *discordgo.InteractionCreate) {
	if i.Member == nil {
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ This command can only be used in a server.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	instances, err := b.instanceRepo.GetAll(b.ctx)
	if err != nil {
		slog.Error("Failed to fetch instances", "error", err)
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ Failed to fetch instances.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	if len(instances) == 0 {
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ No instances found.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
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

	_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
		Type: discordgo.InteractionResponseChannelMessageWithSource,
		Data: &discordgo.InteractionResponseData{
			Embeds:     []*discordgo.MessageEmbed{embed},
			Components: components,
		},
	})
}
