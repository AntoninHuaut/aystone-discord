package api

import (
	"encoding/json"
	"fmt"
	"log/slog"
	"net/url"

	"github.com/antoninhuaut/aystone-discord/internal/config"
	"github.com/google/uuid"
)

type AystoneAPI struct {
	config config.AystoneConfig
}

func NewAystoneAPI(cfg config.AystoneConfig) *AystoneAPI {
	return &AystoneAPI{config: cfg}
}

type UserIdentityResponse[T any] struct {
	ID       *T      `json:"id"`
	Username *string `json:"username"`
}

type UserIdentitiesResponse struct {
	Discord   UserIdentityResponse[string]    `json:"discord"`
	Microsoft UserIdentityResponse[uuid.UUID] `json:"microsoft"`
	Twitch    UserIdentityResponse[string]    `json:"twitch"`
}

func (a *AystoneAPI) GetUserByIdpID(idpSource, idpUserID string) (*UserIdentitiesResponse, error) {
	params := url.Values{}
	params.Set("provider", idpSource)
	params.Set("id", idpUserID)

	targetURL := fmt.Sprintf("%s/discord/player-info?%s", a.config.URL, params.Encode())

	headers := map[string]string{
		"Authorization": fmt.Sprintf("Token %s", a.config.Token),
	}

	body, err := Get(targetURL, headers)
	if err != nil {
		slog.Error("Failed to fetch user from Aystone API", "error", err, "provider", idpSource, "id", idpUserID)
		return nil, err
	}

	var response UserIdentitiesResponse
	if err := json.Unmarshal([]byte(body), &response); err != nil {
		slog.Error("Failed to unmarshal Aystone API response", "error", err)
		return nil, fmt.Errorf("failed to unmarshal response: %w", err)
	}

	return &response, nil
}
