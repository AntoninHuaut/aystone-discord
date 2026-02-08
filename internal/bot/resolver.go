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
	IdentityMicrosoft = "microsoft"
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

func (r *PlayerResolver) OnGuildMembersChunk(s *discordgo.Session, event *discordgo.GuildMembersChunk) {
	slog.Info("Guild members loaded", "guild", event.GuildID, "members", len(event.Members), "chunkIndex", event.ChunkIndex, "chunkCount", event.ChunkCount)
}

func (r *PlayerResolver) ResolveFromDiscord(s *discordgo.Session, input string) (*model.AypiPlayer, error) {
	userID := input
	if !isNumeric(input) {
		userID = r.findUserByName(s, input)
	}

	return r.resolveByIdentity(s, IdentityDiscord, userID)
}

func (r *PlayerResolver) findUserByName(s *discordgo.Session, name string) string {
	s.State.RLock()
	defer s.State.RUnlock()

	for _, guild := range s.State.Guilds {
		if guild == nil {
			continue
		}

		for _, member := range guild.Members {
			if member.User == nil {
				continue
			}

			if strings.EqualFold(member.User.Username, name) ||
				(member.User.GlobalName != "" && strings.EqualFold(member.User.GlobalName, name)) {
				return member.User.ID
			}
		}
	}

	return name
}

func (r *PlayerResolver) ResolveFromMicrosoft(s *discordgo.Session, input string) (*model.AypiPlayer, error) {
	mcUUID := input
	_, err := uuid.Parse(input)
	isUUID := err == nil

	if !isUUID {
		playerInfo, err := api.GetByNameOrUUID(input)
		if err != nil {
			return nil, fmt.Errorf("failed to resolve Minecraft username '%s': %w", input, err)
		}
		if playerInfo == nil {
			return nil, fmt.Errorf("minecraft player '%s' not found", input)
		}
		mcUUID = playerInfo.ID
	}

	return r.resolveByIdentity(s, IdentityMicrosoft, mcUUID)
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
	if len(s) == 0 {
		return false
	}
	for _, c := range s {
		if c < '0' || c > '9' {
			return false
		}
	}
	return true
}
