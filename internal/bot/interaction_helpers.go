package bot

import (
	"fmt"
	"log/slog"

	"github.com/antoninhuaut/aystone-discord/internal/model"
	"github.com/bwmarrin/discordgo"
)

// RespondError sends an ephemeral error message to the user
func RespondError(s *discordgo.Session, i *discordgo.InteractionCreate, message string) {
	err := s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
		Type: discordgo.InteractionResponseChannelMessageWithSource,
		Data: &discordgo.InteractionResponseData{
			Content: message,
			Flags:   discordgo.MessageFlagsEphemeral,
		},
	})
	if err != nil {
		slog.Warn("Failed to send error response", "error", err, "message", message)
	}
}

// CheckPermission validates permission and responds with error if denied
func CheckPermission(s *discordgo.Session, i *discordgo.InteractionCreate, rolesID []string) bool {
	if !HasPermission(i, rolesID) {
		RespondError(s, i, "❌ You do not have permission to use this command.")
		return false
	}
	return true
}

// ParseSingleOption extracts and validates exactly one option from command data
func ParseSingleOption(options []*discordgo.ApplicationCommandInteractionDataOption) (name, value string, err error) {
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
		return "", "", fmt.Errorf("please provide exactly one option: discord, microsoft, or twitch")
	}

	if optionCount > 1 {
		return "", "", fmt.Errorf("please provide exactly one option, not multiple")
	}

	return optionName, optionValue, nil
}

// ResolvePlayerByIdentity resolves a player based on identity type
func (b *Bot) ResolvePlayerByIdentity(s *discordgo.Session, identityType, value string) (*model.AypiPlayer, error) {
	switch identityType {
	case "discord":
		return b.resolver.ResolveFromDiscord(s, value)
	case "microsoft":
		return b.resolver.ResolveFromMicrosoft(s, value)
	case "twitch":
		return b.resolver.ResolveFromTwitch(s, value)
	default:
		return nil, fmt.Errorf("invalid identity type: %s", identityType)
	}
}
