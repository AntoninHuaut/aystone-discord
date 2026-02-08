package bot

import (
	"context"
	"fmt"
	"log/slog"
	"strings"
	"sync"
	"time"

	"github.com/bwmarrin/discordgo"
)

const (
	paginatorTimeout         = 30 * time.Minute
	cleanupInterval          = 5 * time.Minute
	additionalButtonsMinPage = 3
)

type PaginationEmojis struct {
	First    string
	Previous string
	Next     string
	Last     string
}

var defaultEmojis = PaginationEmojis{
	First:    "⏮️",
	Previous: "⏪",
	Next:     "⏩",
	Last:     "⏭️",
}

type PaginatorData struct {
	CurrentPage int
	TotalPages  int
	CreatedAt   time.Time
	BuildPage   func(page int) *discordgo.MessageEmbed
}

var (
	activePaginators = make(map[string]*PaginatorData)
	paginatorMu      sync.Mutex
	cleanupCancel    context.CancelFunc
)

func StartPaginationCleanup(ctx context.Context) {
	cleanupCtx, cancel := context.WithCancel(ctx)
	cleanupCancel = cancel

	go func() {
		ticker := time.NewTicker(cleanupInterval)
		defer ticker.Stop()

		for {
			select {
			case <-cleanupCtx.Done():
				return
			case <-ticker.C:
				cleanupOldPaginators()
			}
		}
	}()
}

func StopPaginationCleanup() {
	if cleanupCancel != nil {
		cleanupCancel()
	}
}

func cleanupOldPaginators() {
	paginatorMu.Lock()
	defer paginatorMu.Unlock()

	cutoff := time.Now().Add(-paginatorTimeout)
	var keysToRemove []string

	for key, data := range activePaginators {
		if data.CreatedAt.Before(cutoff) {
			keysToRemove = append(keysToRemove, key)
		}
	}

	for _, key := range keysToRemove {
		delete(activePaginators, key)
	}

	if len(keysToRemove) > 0 {
		slog.Debug("Cleaned up old paginators", "count", len(keysToRemove))
	}
}

func CreatePagination(userID, messageID, prefix string, totalItems, itemsPerPage int, buildPage func(page int) *discordgo.MessageEmbed) (*discordgo.MessageEmbed, []discordgo.MessageComponent) {
	totalPages := (totalItems + itemsPerPage - 1) / itemsPerPage
	if totalPages == 0 {
		totalPages = 1
	}

	key := fmt.Sprintf("%s_%s", userID, messageID)

	paginatorMu.Lock()
	activePaginators[key] = &PaginatorData{
		CurrentPage: 0,
		TotalPages:  totalPages,
		CreatedAt:   time.Now(),
		BuildPage:   buildPage,
	}
	paginatorMu.Unlock()

	embed := buildPage(0)
	buttons := buildPaginationButtons(key, prefix, 0, totalPages)

	return embed, []discordgo.MessageComponent{
		discordgo.ActionsRow{Components: buttons},
	}
}

func buildPaginationButtons(key, prefix string, currentPage, totalPages int) []discordgo.MessageComponent {
	var buttons []discordgo.MessageComponent

	if totalPages >= additionalButtonsMinPage {
		buttons = append(buttons, discordgo.Button{
			CustomID: fmt.Sprintf("%s:%s:first:%d", prefix, key, currentPage),
			Label:    defaultEmojis.First,
			Style:    discordgo.SecondaryButton,
			Disabled: currentPage == 0,
		})
	}

	buttons = append(buttons, discordgo.Button{
		CustomID: fmt.Sprintf("%s:%s:prev:%d", prefix, key, currentPage),
		Label:    defaultEmojis.Previous,
		Style:    discordgo.PrimaryButton,
		Disabled: currentPage == 0,
	})

	buttons = append(buttons, discordgo.Button{
		CustomID: fmt.Sprintf("%s:%s:info:%d", prefix, key, currentPage),
		Label:    fmt.Sprintf("%d/%d", currentPage+1, totalPages),
		Style:    discordgo.SecondaryButton,
		Disabled: true,
	})

	buttons = append(buttons, discordgo.Button{
		CustomID: fmt.Sprintf("%s:%s:next:%d", prefix, key, currentPage),
		Label:    defaultEmojis.Next,
		Style:    discordgo.PrimaryButton,
		Disabled: currentPage >= totalPages-1,
	})

	if totalPages >= additionalButtonsMinPage {
		buttons = append(buttons, discordgo.Button{
			CustomID: fmt.Sprintf("%s:%s:last:%d", prefix, key, currentPage),
			Label:    defaultEmojis.Last,
			Style:    discordgo.SecondaryButton,
			Disabled: currentPage >= totalPages-1,
		})
	}

	return buttons
}

func HandlePaginationButton(s *discordgo.Session, i *discordgo.InteractionCreate, prefix string, rolesID []string) bool {
	if i.Type != discordgo.InteractionMessageComponent {
		return false
	}

	customID := i.MessageComponentData().CustomID
	if !strings.HasPrefix(customID, prefix+":") {
		return false
	}

	if !HasPermission(i, rolesID) {
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ You do not have permission to use this command.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return true
	}

	parts := strings.Split(customID, ":")
	if len(parts) < 4 {
		return false
	}

	key := parts[1]
	action := parts[2]

	paginatorMu.Lock()
	paginator, exists := activePaginators[key]
	paginatorMu.Unlock()

	if !exists {
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseChannelMessageWithSource,
			Data: &discordgo.InteractionResponseData{
				Content: "❌ This pagination session has expired. Please run the command again.",
				Flags:   discordgo.MessageFlagsEphemeral,
			},
		})
		return true
	}

	newPage := paginator.CurrentPage
	switch action {
	case "first":
		newPage = 0
	case "prev":
		newPage = max(0, paginator.CurrentPage-1)
	case "next":
		newPage = min(paginator.TotalPages-1, paginator.CurrentPage+1)
	case "last":
		newPage = paginator.TotalPages - 1
	case "info":
		_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
			Type: discordgo.InteractionResponseDeferredMessageUpdate,
		})
		return true
	default:
		return false
	}

	paginatorMu.Lock()
	paginator.CurrentPage = newPage
	paginatorMu.Unlock()

	embed := paginator.BuildPage(newPage)
	buttons := buildPaginationButtons(key, prefix, newPage, paginator.TotalPages)

	_ = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
		Type: discordgo.InteractionResponseUpdateMessage,
		Data: &discordgo.InteractionResponseData{
			Embeds: []*discordgo.MessageEmbed{embed},
			Components: []discordgo.MessageComponent{
				discordgo.ActionsRow{Components: buttons},
			},
		},
	})

	return true
}
