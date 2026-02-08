package repository

import (
	"context"
	"database/sql"
	"errors"

	"github.com/antoninhuaut/aystone-discord/internal/database"
	"github.com/antoninhuaut/aystone-discord/internal/model"
	"github.com/google/uuid"
)

type PlayerRepository struct {
	db *database.DB
}

func NewPlayerRepository(db *database.DB) *PlayerRepository {
	return &PlayerRepository{db: db}
}

func (r *PlayerRepository) GetByUUID(ctx context.Context, uuid uuid.UUID) (*model.AystonePlayer, error) {
	query := `
		SELECT uuid, whitelist, ban, instance, created_on, last_login
		FROM aystone_players
		WHERE uuid = $1
	`

	var player model.AystonePlayer
	if err := r.db.GetContext(ctx, &player, query, uuid); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}

	return &player, nil
}
