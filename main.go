package main

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"os/signal"
	"syscall"

	"github.com/antoninhuaut/aystone-discord/internal/api"
	"github.com/antoninhuaut/aystone-discord/internal/bot"
	"github.com/antoninhuaut/aystone-discord/internal/config"
	"github.com/antoninhuaut/aystone-discord/internal/database"
	"github.com/antoninhuaut/aystone-discord/internal/repository"
)

func main() {
	logger := slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{
		Level: slog.LevelInfo,
	}))
	slog.SetDefault(logger)

	slog.Info("Starting Aystone Discord Bot")

	cfg, err := config.LoadConfig()
	if err != nil {
		slog.Error("Failed to load configuration", "error", err)
		os.Exit(1)
	}

	slog.Info("Configuration loaded successfully")

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	db, err := database.Connect(ctx, cfg.Database)
	if err != nil {
		slog.Error("Failed to connect to database", "error", err)
		os.Exit(1)
	}
	defer db.Close()

	instanceRepo := repository.NewInstanceRepository(db)
	playerRepo := repository.NewPlayerRepository(db)
	sanctionRepo := repository.NewSanctionRepository(db)

	aystoneAPI := api.NewAystoneAPI(cfg.Aystone)
	twitchAPI := api.NewTwitchAPI(cfg.Twitch)

	discordBot, err := bot.NewBot(
		ctx,
		cfg.Bot,
		aystoneAPI,
		twitchAPI,
		instanceRepo,
		playerRepo,
		sanctionRepo,
	)
	if err != nil {
		slog.Error("Failed to create Discord bot", "error", err)
		os.Exit(1)
	}

	if err := discordBot.Start(); err != nil {
		slog.Error("Failed to start Discord bot", "error", err)
		os.Exit(1)
	}

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, os.Interrupt, syscall.SIGTERM)

	fmt.Println("Bot is running. Press Ctrl+C to stop.")
	<-stop

	slog.Info("Shutting down...")
	discordBot.Stop()
	slog.Info("Shutdown complete")
}
