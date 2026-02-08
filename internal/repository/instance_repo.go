package repository

import (
	"context"

	"github.com/antoninhuaut/aystone-discord/internal/database"
	"github.com/antoninhuaut/aystone-discord/internal/model"
)

type InstanceRepository struct {
	db *database.DB
}

func NewInstanceRepository(db *database.DB) *InstanceRepository {
	return &InstanceRepository{db: db}
}

func (r *InstanceRepository) GetAll(ctx context.Context) ([]model.AystoneInstance, error) {
	query := `
		SELECT 
			i.name,
			i.visible,
			i.max_player,
			i.ip_address,
			i.port,
			i.enabled,
			i.cnfapplied,
			COALESCE(COUNT(p.uuid), 0) as number_registered_players
		FROM aystone_instances i
		LEFT JOIN aystone_players p ON p.instance = i.name
		GROUP BY i.name, i.visible, i.max_player, i.ip_address, i.port, i.enabled, i.cnfapplied
		ORDER BY i.name
	`

	var instances []model.AystoneInstance
	if err := r.db.SelectContext(ctx, &instances, query); err != nil {
		return nil, err
	}

	return instances, nil
}
