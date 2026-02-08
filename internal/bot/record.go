package bot

import (
	"fmt"
	"log/slog"

	"github.com/bwmarrin/discordgo"
)

func (b *Bot) handleRecord(s *discordgo.Session, i *discordgo.InteractionCreate) {
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

	b.sanctionHandler.SendRecords(s, i, aypiPlayer.McUUID, b.ctx)
}

func (b *Bot) handleRecordContext(s *discordgo.Session, i *discordgo.InteractionCreate) {
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

	b.sanctionHandler.SendRecords(s, i, aypiPlayer.McUUID, b.ctx)
}
