package bot

import (
	"fmt"
	"time"

	"github.com/antoninhuaut/aystone-discord/internal/model"
	"github.com/bwmarrin/discordgo"
)

const (
	minotarURL = "https://minotar.net/avatar"
	embedColor = 0x1ABC9C
	dateFormat = "02/01/2006 15:04"
)

func BuildPlayerInfoEmbed(player *model.AystonePlayer, aypiPlayer *model.AypiPlayer, mcName string) *discordgo.MessageEmbed {
	instanceName := "None"
	if player.InstanceName != nil {
		instanceName = *player.InstanceName
	}

	whitelistStatus := "✅ Yes"
	if !player.Whitelist {
		whitelistStatus = "❌ No"
	}

	banStatus := "🟢 No"
	if player.Ban {
		banStatus = "🚫 Yes"
	}

	return &discordgo.MessageEmbed{
		Title:     fmt.Sprintf("👤 Player Info: `%s`", mcName),
		Color:     embedColor,
		Thumbnail: &discordgo.MessageEmbedThumbnail{URL: fmt.Sprintf("%s/%s.png", minotarURL, player.UUID)},
		Fields: []*discordgo.MessageEmbedField{
			{Name: "UUID", Value: player.UUID.String(), Inline: false},
			{Name: "Whitelist", Value: whitelistStatus, Inline: true},
			{Name: "Banned", Value: banStatus, Inline: true},
			{Name: "Instance", Value: instanceName, Inline: true},
			{Name: "Created On", Value: player.CreatedOn.Format(dateFormat), Inline: true},
			{Name: "Last Login", Value: player.LastLogin.Format(dateFormat), Inline: true},
			{Name: "\u200B", Value: "\u200B", Inline: true},
			{Name: "Twitch", Value: fmt.Sprintf("`%s` (`%s`)", aypiPlayer.TwitchName, aypiPlayer.TwitchID), Inline: true},
			{Name: "Discord", Value: fmt.Sprintf("`%s` (`%s`)", aypiPlayer.DiscordName, aypiPlayer.DiscordID), Inline: true},
		},
		Footer:    &discordgo.MessageEmbedFooter{Text: "Aystone"},
		Timestamp: time.Now().Format(time.RFC3339),
	}
}

func BuildSanctionField(sanction model.AystoneSanction, embed *discordgo.MessageEmbed) *discordgo.MessageEmbed {
	reason := "No reason"
	if sanction.Reason != nil {
		reason = *sanction.Reason
	}

	value := fmt.Sprintf("*%s*\n%s", sanction.SanctionApplied.Format(dateFormat), reason)

	embed.Fields = append(embed.Fields, &discordgo.MessageEmbedField{
		Name:   string(sanction.Type),
		Value:  value,
		Inline: true,
	})

	return embed
}

func BuildInstanceField(instance model.AystoneInstance, embed *discordgo.MessageEmbed) *discordgo.MessageEmbed {
	enabledStatus := "❌ Disabled"
	if instance.Enabled {
		enabledStatus = "✅ Enabled"
	}

	visibleStatus := "❌ Invisible"
	if instance.Visible {
		visibleStatus = "✅ Visible"
	}

	value := fmt.Sprintf("%d/%d players\n%s\n%s",
		instance.NumberRegisteredPlayers,
		instance.MaxPlayer,
		enabledStatus,
		visibleStatus,
	)

	embed.Fields = append(embed.Fields, &discordgo.MessageEmbedField{
		Name:   instance.Name,
		Value:  value,
		Inline: true,
	})

	return embed
}
