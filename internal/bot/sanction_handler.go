package bot

import (
	"context"
	"fmt"
	"log/slog"
	"strings"

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

func (h *SanctionHandler) HandleButton(s *discordgo.Session, i *discordgo.InteractionCreate, ctx context.Context) bool {
	if i.Type != discordgo.InteractionMessageComponent {
		return false
	}

	customID := i.MessageComponentData().CustomID

	if HandlePaginationButton(s, i, sanctionButtonSee, h.rolesID) {
		return true
	}

	if strings.HasPrefix(customID, sanctionButtonAsk+":") {
		if !HasPermission(i, h.rolesID) {
			RespondError(s, i, "❌ You do not have permission to use this command.")
			return true
		}

		uuidStr := strings.TrimPrefix(customID, sanctionButtonAsk+":")
		playerUUID, err := uuid.Parse(uuidStr)
		if err != nil {
			slog.Error("Invalid UUID format in button", "customID", customID, "uuid", uuidStr, "error", err)
			RespondError(s, i, "❌ Unable to process this action. Please try again.")
			return true
		}

		h.SendRecords(s, i, playerUUID, ctx)
		return true
	}

	return false
}

func (h *SanctionHandler) SendRecords(s *discordgo.Session, i *discordgo.InteractionCreate, playerUUID uuid.UUID, ctx context.Context) {
	if i.Member == nil {
		RespondError(s, i, "❌ This command can only be used in a server.")
		return
	}

	sanctions, err := h.sanctionRepo.GetByUUIDSortDateDesc(ctx, playerUUID)
	if err != nil {
		slog.Error("Failed to fetch sanctions", "error", err)
		RespondError(s, i, "❌ Failed to fetch sanctions.")
		return
	}

	if len(sanctions) == 0 {
		RespondError(s, i, "❌ No sanctions found.")
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

	err = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
		Type: discordgo.InteractionResponseChannelMessageWithSource,
		Data: &discordgo.InteractionResponseData{
			Embeds:     []*discordgo.MessageEmbed{embed},
			Components: components,
		},
	})
	if err != nil {
		slog.Warn("Failed to send sanction list response", "error", err)
	}
}
