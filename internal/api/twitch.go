package api

import (
	"encoding/json"
	"fmt"
	"log/slog"
	"net/url"
	"sync"
	"time"

	"github.com/antoninhuaut/aystone-discord/internal/config"
)

type TwitchAPI struct {
	config   config.TwitchConfig
	authData *authResponse
	mu       sync.Mutex
}

func NewTwitchAPI(cfg config.TwitchConfig) *TwitchAPI {
	return &TwitchAPI{config: cfg}
}

type authResponse struct {
	AccessToken string    `json:"access_token"`
	ExpiresIn   int       `json:"expires_in"`
	TokenType   string    `json:"token_type"`
	IssuedAt    time.Time `json:"-"`
}

type usersResponse struct {
	Data []struct {
		ID    string `json:"id"`
		Login string `json:"login"`
	} `json:"data"`
}

func (t *TwitchAPI) GetUserID(username string) (string, error) {
	params := url.Values{}
	params.Set("login", username)
	targetURL := fmt.Sprintf("%s/users?%s", t.config.APIURL, params.Encode())

	body, err := t.getAuth(targetURL)
	if err != nil {
		slog.Error("Failed to fetch Twitch user", "error", err, "username", username)
		return "", err
	}

	var response usersResponse
	if err := json.Unmarshal([]byte(body), &response); err != nil {
		slog.Error("Failed to unmarshal Twitch response", "error", err)
		return "", fmt.Errorf("failed to unmarshal response: %w", err)
	}

	if len(response.Data) == 0 {
		return "", fmt.Errorf("no user found with username: %s", username)
	}

	return response.Data[0].ID, nil
}

func (t *TwitchAPI) auth() (*authResponse, error) {
	params := map[string]string{
		"client_id":     t.config.ClientID,
		"client_secret": t.config.ClientSecret,
		"grant_type":    "client_credentials",
	}

	body, err := PostFormData(t.config.TokenURL, params)
	if err != nil {
		return nil, fmt.Errorf("failed to authenticate: %w", err)
	}

	var response authResponse
	if err := json.Unmarshal([]byte(body), &response); err != nil {
		return nil, fmt.Errorf("failed to unmarshal auth response: %w", err)
	}

	response.IssuedAt = time.Now()
	return &response, nil
}

func (t *TwitchAPI) getAuth(targetURL string) (string, error) {
	t.mu.Lock()
	defer t.mu.Unlock()

	if t.authData == nil || time.Since(t.authData.IssuedAt) >= time.Duration(t.authData.ExpiresIn)*time.Second {
		authResp, err := t.auth()
		if err != nil {
			slog.Error("Failed to authenticate with Twitch", "error", err)
			return "", err
		}
		t.authData = authResp
	}

	headers := map[string]string{
		"Client-ID":     t.config.ClientID,
		"Authorization": fmt.Sprintf("Bearer %s", t.authData.AccessToken),
	}

	return Get(targetURL, headers)
}
