package model

import (
	"github.com/google/uuid"
)

type AypiPlayer struct {
	DiscordID   string
	DiscordName string
	McUUID      uuid.UUID
	McName      string
	TwitchID    string
	TwitchName  string
}
