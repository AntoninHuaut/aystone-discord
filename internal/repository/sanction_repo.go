package repository

import (
	"context"

	"github.com/antoninhuaut/aystone-discord/internal/database"
	"github.com/antoninhuaut/aystone-discord/internal/model"
	"github.com/google/uuid"
)

type SanctionRepository struct {
	db *database.DB
}

func NewSanctionRepository(db *database.DB) *SanctionRepository {
	return &SanctionRepository{db: db}
}

func (r *SanctionRepository) GetByUUIDSortDateDesc(ctx context.Context, uuid uuid.UUID) ([]model.AystoneSanction, error) {
	query := `
		SELECT sanction_id, player_uuid, type, reason, until, sanction_applied
		FROM aystone_sanctions
		WHERE player_uuid = $1
		ORDER BY sanction_applied DESC
	`

	var sanctions []model.AystoneSanction
	if err := r.db.SelectContext(ctx, &sanctions, query, uuid); err != nil {
		return nil, err
	}

	return sanctions, nil
}

func (r *SanctionRepository) CountByUUID(ctx context.Context, uuid uuid.UUID) (int64, error) {
	query := `
		SELECT COUNT(*)
		FROM aystone_sanctions
		WHERE player_uuid = $1
	`

	var count int64
	if err := r.db.GetContext(ctx, &count, query, uuid); err != nil {
		return 0, err
	}

	return count, nil
}
