package bot

import (
	"context"
	"fmt"
	"log/slog"

	"github.com/antoninhuaut/aystone-discord/internal/api"
	"github.com/antoninhuaut/aystone-discord/internal/config"
	"github.com/antoninhuaut/aystone-discord/internal/repository"
	"github.com/bwmarrin/discordgo"
)

type Bot struct {
	session         *discordgo.Session
	config          config.BotConfig
	resolver        *PlayerResolver
	sanctionHandler *SanctionHandler
	instanceRepo    *repository.InstanceRepository
	playerRepo      *repository.PlayerRepository
	ctx             context.Context
}

func NewBot(ctx context.Context, cfg config.BotConfig, aystoneAPI *api.AystoneAPI, twitchAPI *api.TwitchAPI, instanceRepo *repository.InstanceRepository, playerRepo *repository.PlayerRepository, sanctionRepo *repository.SanctionRepository) (*Bot, error) {
	session, err := discordgo.New("Bot " + cfg.Token)
	if err != nil {
		return nil, fmt.Errorf("failed to create Discord session: %w", err)
	}

	session.Identify.Intents = discordgo.IntentsGuilds | discordgo.IntentsGuildMembers

	bot := &Bot{
		session:         session,
		config:          cfg,
		resolver:        NewPlayerResolver(aystoneAPI, twitchAPI),
		sanctionHandler: NewSanctionHandler(sanctionRepo, cfg.RolesID),
		instanceRepo:    instanceRepo,
		playerRepo:      playerRepo,
		ctx:             ctx,
	}

	session.AddHandler(bot.onReady)
	session.AddHandler(bot.onInteractionCreate)
	session.AddHandler(bot.resolver.OnGuildMembersChunk)

	StartPaginationCleanup(ctx)

	return bot, nil
}

func (b *Bot) Start() error {
	if err := b.session.Open(); err != nil {
		return fmt.Errorf("failed to open Discord session: %w", err)
	}

	if b.config.Activity != "" {
		_ = b.session.UpdateGameStatus(0, b.config.Activity)
	}

	slog.Info("Bot is now running")
	return nil
}

func (b *Bot) Stop() {
	StopPaginationCleanup()
	if b.session != nil {
		_ = b.session.Close()
	}
	slog.Info("Bot stopped")
}

func (b *Bot) onReady(s *discordgo.Session, r *discordgo.Ready) {
	slog.Info("Bot is ready", "user", r.User.Username)

	for _, guild := range r.Guilds {
		b.registerCommands(guild.ID)

		err := s.RequestGuildMembers(guild.ID, "", 0, "", false)
		if err != nil {
			slog.Error("Failed to request guild members", "guild", guild.ID, "error", err)
		} else {
			slog.Info("Requested guild members for caching", "guild", guild.ID)
		}
	}
}

func (b *Bot) registerCommands(guildID string) {
	commands := []*discordgo.ApplicationCommand{
		{
			Name:        "whois",
			Description: "Displays information about a player",
			Options: []*discordgo.ApplicationCommandOption{
				{
					Type:        discordgo.ApplicationCommandOptionString,
					Name:        IdentityDiscord,
					Description: "Discord id or name",
					Required:    false,
				},
				{
					Type:        discordgo.ApplicationCommandOptionString,
					Name:        IdentityMicrosoft,
					Description: "Minecraft name or uuid",
					Required:    false,
				},
				{
					Type:        discordgo.ApplicationCommandOptionString,
					Name:        IdentityTwitch,
					Description: "Twitch id or name",
					Required:    false,
				},
			},
		},
		{
			Name:        "record",
			Description: "Displays records about a player",
			Options: []*discordgo.ApplicationCommandOption{
				{
					Type:        discordgo.ApplicationCommandOptionString,
					Name:        IdentityDiscord,
					Description: "Discord id or name",
					Required:    false,
				},
				{
					Type:        discordgo.ApplicationCommandOptionString,
					Name:        IdentityMicrosoft,
					Description: "Minecraft name or uuid",
					Required:    false,
				},
				{
					Type:        discordgo.ApplicationCommandOptionString,
					Name:        IdentityTwitch,
					Description: "Twitch id or name",
					Required:    false,
				},
			},
		},
		{
			Name:        "instance",
			Description: "Instance command",
			Options: []*discordgo.ApplicationCommandOption{
				{
					Type:        discordgo.ApplicationCommandOptionSubCommand,
					Name:        "list",
					Description: "List all instances",
				},
			},
		},
	}

	userCommands := []*discordgo.ApplicationCommand{
		{
			Name: "Aystone Player Info",
			Type: discordgo.UserApplicationCommand,
		},
		{
			Name: "Aystone Player Records",
			Type: discordgo.UserApplicationCommand,
		},
	}

	allCommands := append(commands, userCommands...)
	_, err := b.session.ApplicationCommandBulkOverwrite(b.session.State.User.ID, guildID, allCommands)
	if err != nil {
		slog.Error("Failed to register commands", "guild", guildID, "error", err)
		return
	}

	slog.Info("Registered commands", "guild", guildID, "count", len(allCommands))
}

func (b *Bot) onInteractionCreate(s *discordgo.Session, i *discordgo.InteractionCreate) {
	if i.Type == discordgo.InteractionMessageComponent {
		if b.sanctionHandler.HandleButton(s, i, b.ctx) {
			return
		}

		if HandlePaginationButton(s, i, "instance_list", b.config.RolesID) {
			return
		}

		slog.Warn("Unhandled button interaction", "customID", i.MessageComponentData().CustomID)
		return
	}

	if i.Type == discordgo.InteractionApplicationCommand {
		data := i.ApplicationCommandData()

		switch data.Name {
		case "whois":
			b.handleWhois(s, i)
		case "record":
			b.handleRecord(s, i)
		case "instance":
			b.handleInstance(s, i)
		case "Aystone Player Info":
			b.handleWhoisContext(s, i)
		case "Aystone Player Records":
			b.handleRecordContext(s, i)
		}
	}
}
