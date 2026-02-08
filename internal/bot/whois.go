package bot

import (
	"fmt"
	"log/slog"

	"github.com/antoninhuaut/aystone-discord/internal/api"
	"github.com/antoninhuaut/aystone-discord/internal/model"
	"github.com/bwmarrin/discordgo"
)

func (b *Bot) handleWhois(s *discordgo.Session, i *discordgo.InteractionCreate) {
	if !CheckPermission(s, i, b.config.RolesID) {
		return
	}

	data := i.ApplicationCommandData()
	optionName, optionValue, err := ParseSingleOption(data.Options)
	if err != nil {
		RespondError(s, i, "❌ "+err.Error())
		return
	}

	aypiPlayer, err := b.ResolvePlayerByIdentity(s, optionName, optionValue)
	if err != nil {
		slog.Error("Failed to resolve player", "error", err, "identity", optionName, "value", optionValue)
		RespondError(s, i, fmt.Sprintf("❌ Error while resolving player: %v", err))
		return
	}

	b.displayWhois(s, i, aypiPlayer)
}

func (b *Bot) handleWhoisContext(s *discordgo.Session, i *discordgo.InteractionCreate) {
	if !CheckPermission(s, i, b.config.RolesID) {
		return
	}

	data := i.ApplicationCommandData()
	targetUser := data.TargetID

	aypiPlayer, err := b.resolver.ResolveFromDiscord(s, targetUser)
	if err != nil {
		slog.Error("Failed to resolve player from context", "error", err, "targetUser", targetUser)
		RespondError(s, i, "❌ No AypiPlayer found for this user.")
		return
	}

	b.displayWhois(s, i, aypiPlayer)
}

func (b *Bot) displayWhois(s *discordgo.Session, i *discordgo.InteractionCreate, aypiPlayer *model.AypiPlayer) {
	player, err := b.playerRepo.GetByUUID(b.ctx, aypiPlayer.McUUID)
	if err != nil {
		slog.Error("Failed to fetch player from database", "error", err, "uuid", aypiPlayer.McUUID)
		RespondError(s, i, "❌ Database error while fetching player information.")
		return
	}

	if player == nil {
		slog.Info("Player not found in database", "uuid", aypiPlayer.McUUID)
		RespondError(s, i, "❌ Aystone Player not found. The player may have never joined the server.")
		return
	}

	mcInfo, err := api.GetByNameOrUUID(player.UUID.String())
	mcName := player.UUID.String()
	if err == nil && mcInfo != nil {
		mcName = mcInfo.Username
	}

	embed := BuildPlayerInfoEmbed(player, aypiPlayer, mcName)

	buttons := b.sanctionHandler.CreateSanctionButtons(b.ctx, player.UUID)

	err = s.InteractionRespond(i.Interaction, &discordgo.InteractionResponse{
		Type: discordgo.InteractionResponseChannelMessageWithSource,
		Data: &discordgo.InteractionResponseData{
			Embeds: []*discordgo.MessageEmbed{embed},
			Components: []discordgo.MessageComponent{
				discordgo.ActionsRow{Components: buttons},
			},
		},
	})
	if err != nil {
		slog.Warn("Failed to send whois response", "error", err)
	}
}
