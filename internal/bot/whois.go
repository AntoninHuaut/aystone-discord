package bot

import (
	"fmt"
	"log/slog"

	"github.com/antoninhuaut/aystone-discord/internal/api"
	"github.com/antoninhuaut/aystone-discord/internal/model"
	"github.com/bwmarrin/discordgo"
)

func (b *Bot) handleWhois(s *discordgo.Session, i *discordgo.InteractionCreate) {
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
	options := data.Options

	var optionName, optionValue string
	optionCount := 0
	for _, opt := range options {
		if opt.StringValue() != "" {
			optionCount++
			if optionCount == 1 {
				optionName = opt.Name
				optionValue = opt.StringValue()
			}
		}
	}

	if optionCount == 0 {
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ Please provide exactly one option: discord, microsoft, or twitch.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	if optionCount > 1 {
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ Please provide exactly one option, not multiple.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	var aypiPlayer *model.AypiPlayer
	var err error

	switch optionName {
	case IdentityDiscord:
		aypiPlayer, err = b.resolver.ResolveFromDiscord(s, optionValue)
	case IdentityMicrosoft:
		aypiPlayer, err = b.resolver.ResolveFromMicrosoft(s, optionValue)
	case IdentityTwitch:
		aypiPlayer, err = b.resolver.ResolveFromTwitch(s, optionValue)
	default:
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ Invalid option provided.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	if err != nil {
		slog.Error("Failed to resolve player", "error", err)
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: fmt.Sprintf("❌ Error while resolving player: %v", err),
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	b.displayWhois(s, i, aypiPlayer)
}

func (b *Bot) handleWhoisContext(s *discordgo.Session, i *discordgo.InteractionCreate) {
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
	targetUser := data.TargetID

	aypiPlayer, err := b.resolver.ResolveFromDiscord(s, targetUser)
	if err != nil {
		slog.Error("Failed to resolve player from context", "error", err)
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ No AypiPlayer found for this user.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	b.displayWhois(s, i, aypiPlayer)
}

func (b *Bot) displayWhois(s *discordgo.Session, i *discordgo.InteractionCreate, aypiPlayer *model.AypiPlayer) {
	player, err := b.playerRepo.GetByUUID(b.ctx, aypiPlayer.McUUID)
	if err != nil {
		slog.Error("Failed to fetch player from database", "error", err, "uuid", aypiPlayer.McUUID)
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ Database error while fetching player information.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	if player == nil {
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ Aystone Player not found. The player may have never joined the server.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	mcInfo, err := api.GetByNameOrUUID(player.UUID.String())
	mcName := player.UUID.String()
	if err == nil && mcInfo != nil {
		mcName = mcInfo.Username
	}

	embed := BuildPlayerInfoEmbed(player, aypiPlayer, mcName)

	buttons := b.sanctionHandler.CreateSanctionButtons(b.ctx, player.UUID)

	_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
		Type: discordgo.InteractionResponseChannelMessageWithSource,
		Data: &discordgo.InteractionResponseData{
			Embeds: []*discordgo.MessageEmbed{embed},
			Components: []discordgo.MessageComponent{
				discordgo.ActionsRow{Components: buttons},
			},
		},
	})
}
