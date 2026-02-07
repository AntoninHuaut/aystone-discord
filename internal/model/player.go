package model

import (
	"time"

	"github.com/google/uuid"
)

type AystonePlayer struct {
	UUID         uuid.UUID `db:"uuid"`
	Whitelist    bool      `db:"whitelist"`
	Ban          bool      `db:"ban"`
	InstanceName *string   `db:"instance"`
	CreatedOn    time.Time `db:"created_on"`
	LastLogin    time.Time `db:"last_login"`
}
