package bot

import (
	"fmt"
	"log/slog"

	"github.com/antoninhuaut/aystone-discord/internal/model"
	"github.com/bwmarrin/discordgo"
)

func (b *Bot) handleRecord(s *discordgo.Session, i *discordgo.InteractionCreate) {
	if !HasPermission(i, b.config.RolesID) {
		s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
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

	// Find the provided option
	var optionName, optionValue string
	for _, opt := range options {
		if opt.StringValue() != "" {
			optionName = opt.Name
			optionValue = opt.StringValue()
			break
		}
	}

	if optionValue == "" {
		s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ Please provide exactly one option: discord, minecraft, or twitch.",
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
	case IdentityMinecraft:
		aypiPlayer, err = b.resolver.ResolveFromMinecraft(s, optionValue)
	case IdentityTwitch:
		aypiPlayer, err = b.resolver.ResolveFromTwitch(s, optionValue)
	default:
		s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
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
		s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: fmt.Sprintf("❌ Error while resolving player: %v", err),
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	b.sanctionHandler.SendRecords(s, i, aypiPlayer.McUUID)
}

func (b *Bot) handleRecordContext(s *discordgo.Session, i *discordgo.InteractionCreate) {
	if !HasPermission(i, b.config.RolesID) {
		s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
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
		s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ No AypiPlayer found for this user.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	b.sanctionHandler.SendRecords(s, i, aypiPlayer.McUUID)
}
