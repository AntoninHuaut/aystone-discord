package database

import (
	"context"
	"database/sql"
	"fmt"
	"log/slog"

	"github.com/antoninhuaut/aystone-discord/internal/config"
	_ "github.com/jackc/pgx/v5/stdlib"
	"github.com/jmoiron/sqlx"
)

type DB struct {
	*sqlx.DB
}

func Connect(ctx context.Context, cfg config.DatabaseConfig) (*DB, error) {
	connStr := fmt.Sprintf(
		"host=%s port=%s user=%s password=%s dbname=%s sslmode=disable search_path=%s",
		cfg.Host, cfg.Port, cfg.Username, cfg.Password, cfg.Name, cfg.Schema,
	)

	db, err := sql.Open("pgx", connStr)
	if err != nil {
		return nil, fmt.Errorf("failed to open database: %w", err)
	}

	if err := db.PingContext(ctx); err != nil {
		_ = db.Close()
		return nil, fmt.Errorf("failed to ping database: %w", err)
	}

	db.SetMaxOpenConns(10)
	db.SetMaxIdleConns(5)

	sqlxDB := sqlx.NewDb(db, "pgx")

	slog.Info("Connected to database", "host", cfg.Host, "port", cfg.Port, "database", cfg.Name)

	return &DB{
		DB: sqlxDB,
	}, nil
}

func (db *DB) Close() {
	if db.DB != nil {
		_ = db.DB.Close()
		slog.Info("Database connection closed")
	}
}
