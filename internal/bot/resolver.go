package bot

import (
	"fmt"
	"log/slog"
	"strings"

	"github.com/antoninhuaut/aystone-discord/internal/api"
	"github.com/antoninhuaut/aystone-discord/internal/model"
	"github.com/bwmarrin/discordgo"
	"github.com/google/uuid"
)

const (
	IdentityDiscord   = "discord"
	IdentityMinecraft = "microsoft"
	IdentityTwitch    = "twitch"
)

type PlayerResolver struct {
	aystoneAPI *api.AystoneAPI
	twitchAPI  *api.TwitchAPI
}

func NewPlayerResolver(aystoneAPI *api.AystoneAPI, twitchAPI *api.TwitchAPI) *PlayerResolver {
	return &PlayerResolver{
		aystoneAPI: aystoneAPI,
		twitchAPI:  twitchAPI,
	}
}

func (r *PlayerResolver) ResolveFromDiscord(s *discordgo.Session, input string) (*model.AypiPlayer, error) {
	userID := input
	if !isNumeric(input) {
		guild := s.State.Guilds[0]
		if guild != nil {
			for _, member := range guild.Members {
				if strings.EqualFold(member.User.Username, input) || strings.EqualFold(member.User.GlobalName, input) {
					userID = member.User.ID
					break
				}
			}
		}
	}

	return r.resolveByIdentity(s, IdentityDiscord, userID)
}

func (r *PlayerResolver) ResolveFromMinecraft(s *discordgo.Session, input string) (*model.AypiPlayer, error) {
	mcUUID := input
	playerInfo, err := api.GetByNameOrUUID(input)
	if err == nil && playerInfo != nil {
		mcUUID = playerInfo.ID
	}

	return r.resolveByIdentity(s, IdentityMinecraft, mcUUID)
}

func (r *PlayerResolver) ResolveFromTwitch(s *discordgo.Session, input string) (*model.AypiPlayer, error) {
	twitchID := input
	if !isNumeric(input) {
		id, err := r.twitchAPI.GetUserID(input)
		if err != nil {
			return nil, fmt.Errorf("failed to resolve Twitch name: %w", err)
		}
		twitchID = id
	}

	return r.resolveByIdentity(s, IdentityTwitch, twitchID)
}

func (r *PlayerResolver) resolveByIdentity(s *discordgo.Session, identity, identityID string) (*model.AypiPlayer, error) {
	identities, err := r.aystoneAPI.GetUserByIdpID(identity, identityID)
	if err != nil {
		slog.Error("Failed to fetch identities from Aystone", "error", err, "identity", identity, "id", identityID)
		return nil, fmt.Errorf("failed to fetch identities: %w", err)
	}

	return r.buildAypiPlayer(s, identities), nil
}

func (r *PlayerResolver) buildAypiPlayer(s *discordgo.Session, identities *api.UserIdentitiesResponse) *model.AypiPlayer {
	discordID := ""
	discordName := ""
	if identities.Discord.ID != nil {
		discordID = *identities.Discord.ID
		if identities.Discord.Username != nil {
			discordName = *identities.Discord.Username
		} else {
			user, err := s.User(discordID)
			if err == nil {
				discordName = user.Username
			}
		}
	}

	mcUUID := uuid.Nil
	mcName := ""
	if identities.Microsoft.ID != nil {
		mcUUID = *identities.Microsoft.ID
	} else if identities.Microsoft.Username != nil {
		playerInfo, err := api.GetByNameOrUUID(*identities.Microsoft.Username)
		if err == nil && playerInfo != nil {
			parsedUUID, err := uuid.Parse(playerInfo.ID)
			if err == nil {
				mcUUID = parsedUUID
			}
		}
	}
	if identities.Microsoft.Username != nil {
		mcName = *identities.Microsoft.Username
	}

	twitchID := ""
	twitchName := ""
	if identities.Twitch.ID != nil {
		twitchID = *identities.Twitch.ID
	}
	if identities.Twitch.Username != nil {
		twitchName = *identities.Twitch.Username
	}

	return &model.AypiPlayer{
		DiscordID:   discordID,
		DiscordName: discordName,
		McUUID:      mcUUID,
		McName:      mcName,
		TwitchID:    twitchID,
		TwitchName:  twitchName,
	}
}

func isNumeric(s string) bool {
	for _, c := range s {
		if c < '0' || c > '9' {
			return false
		}
	}
	return len(s) > 0
}
