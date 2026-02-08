package bot

import (
	"context"
	"fmt"
	"log/slog"

	"github.com/antoninhuaut/aystone-discord/internal/api"
	"github.com/antoninhuaut/aystone-discord/internal/repository"
	"github.com/bwmarrin/discordgo"
	"github.com/google/uuid"
)

const (
	sanctionButtonPrefix  = "whois_sanction"
	sanctionButtonAsk     = "whois_sanction_ask"
	sanctionButtonNothing = "whois_sanction_nothing"
	sanctionButtonSee     = "whois_sanction_see"
	sanctionItemsPerPage  = 9
)

type SanctionHandler struct {
	sanctionRepo *repository.SanctionRepository
	rolesID      []string
}

func NewSanctionHandler(sanctionRepo *repository.SanctionRepository, rolesID []string) *SanctionHandler {
	return &SanctionHandler{
		sanctionRepo: sanctionRepo,
		rolesID:      rolesID,
	}
}

func (h *SanctionHandler) CreateSanctionButtons(ctx context.Context, uuid uuid.UUID) []discordgo.MessageComponent {
	count, err := h.sanctionRepo.CountByUUID(ctx, uuid)
	if err != nil {
		slog.Error("Failed to count sanctions", "error", err)
		count = 0
	}

	var buttons []discordgo.MessageComponent

	if count > 0 {
		label := fmt.Sprintf("See the %d sanction", count)
		if count > 1 {
			label = fmt.Sprintf("See the %d sanctions", count)
		}
		buttons = append(buttons, discordgo.Button{
			CustomID: fmt.Sprintf("%s:%s", sanctionButtonAsk, uuid.String()),
			Label:    label,
			Style:    discordgo.PrimaryButton,
			Emoji:    &discordgo.ComponentEmoji{Name: "⚠"},
		})
	} else {
		buttons = append(buttons, discordgo.Button{
			CustomID: fmt.Sprintf("%s:%s", sanctionButtonNothing, uuid.String()),
			Label:    "No sanction",
			Style:    discordgo.SecondaryButton,
			Emoji:    &discordgo.ComponentEmoji{Name: "✅"},
			Disabled: true,
		})
	}

	return buttons
}

func (h *SanctionHandler) HandleButton(s *discordgo.Session, i *discordgo.InteractionCreate) bool {
	if i.Type != discordgo.InteractionMessageComponent {
		return false
	}

	customID := i.MessageComponentData().CustomID

	if HandlePaginationButton(s, i, sanctionButtonSee, h.rolesID) {
		return true
	}

	if len(customID) > len(sanctionButtonAsk) && customID[:len(sanctionButtonAsk)] == sanctionButtonAsk {
		if !HasPermission(i, h.rolesID) {
			_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
				Type: discordgo.InteractionResponseChannelMessageWithSource,
				Data: &discordgo.InteractionResponseData{
					Content: "❌ You do not have permission to use this command.",
					Flags:   discordgo.MessageFlagsEphemeral,
				},
			})
			return true
		}

		uuidStr := customID[len(sanctionButtonAsk)+1:]
		playerUUID, err := uuid.Parse(uuidStr)
		if err != nil {
			slog.Error("Invalid UUID in button", "uuid", uuidStr, "error", err)
			return true
		}

		h.SendRecords(s, i, playerUUID)
		return true
	}

	return false
}

func (h *SanctionHandler) SendRecords(s *discordgo.Session, i *discordgo.InteractionCreate, playerUUID uuid.UUID) {
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

	ctx := context.Background()

	sanctions, err := h.sanctionRepo.GetByUUIDSortDateDesc(ctx, playerUUID)
	if err != nil {
		slog.Error("Failed to fetch sanctions", "error", err)
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ Failed to fetch sanctions.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	if len(sanctions) == 0 {
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ No sanctions found.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return
	}

	playerInfo, err := api.GetByNameOrUUID(playerUUID.String())
	mcName := playerUUID.String()
	if err == nil && playerInfo != nil {
		mcName = playerInfo.Username
	}

	buildPage := func(page int) *discordgo.MessageEmbed {
		start := page * sanctionItemsPerPage
		end := min(start+sanctionItemsPerPage, len(sanctions))

		embed := &discordgo.MessageEmbed{
			Title:     fmt.Sprintf("⚖️ Sanction List: `%s`", mcName),
			Color:     embedColor,
			Thumbnail: &discordgo.MessageEmbedThumbnail{URL: fmt.Sprintf("%s/%s.png", minotarURL, playerUUID)},
			Footer: &discordgo.MessageEmbedFooter{
				Text:    fmt.Sprintf("Requested by %s", i.Member.User.Username),
				IconURL: i.Member.User.AvatarURL(""),
			},
			Fields: []*discordgo.MessageEmbedField{},
		}

		for _, sanction := range sanctions[start:end] {
			BuildSanctionField(sanction, embed)
		}

		return embed
	}

	embed, components := CreatePagination(i.Member.User.ID, i.ID, sanctionButtonSee, len(sanctions), sanctionItemsPerPage, buildPage)

	_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
		Type: discordgo.InteractionResponseChannelMessageWithSource,
		Data: &discordgo.InteractionResponseData{
			Embeds:     []*discordgo.MessageEmbed{embed},
			Components: components,
		},
	})
}
