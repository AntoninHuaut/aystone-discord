package model

import (
	"time"

	"github.com/google/uuid"
)

type SanctionType string

const (
	SanctionTypeBAN   SanctionType = "BAN"
	SanctionTypeUNBAN SanctionType = "UNBAN"
	SanctionTypeKICK  SanctionType = "KICK"
	SanctionTypeWARN  SanctionType = "WARN"
	SanctionTypeNOTE  SanctionType = "NOTE"
)

type AystoneSanction struct {
	SanctionID      int          `db:"sanction_id"`
	PlayerUUID      uuid.UUID    `db:"player_uuid"`
	Type            SanctionType `db:"type"`
	Reason          *string      `db:"reason"`
	Until           *time.Time   `db:"until"`
	SanctionApplied time.Time    `db:"sanction_applied"`
}
