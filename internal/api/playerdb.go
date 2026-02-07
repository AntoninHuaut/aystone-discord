package api

import (
	"encoding/json"
	"fmt"
	"log/slog"
	"net/url"
)

const playerDBURL = "https://playerdb.co/api/player/minecraft/"

type PlayerDBResponse struct {
	Code    string `json:"code"`
	Message string `json:"message"`
	Data    struct {
		Player PlayerInfo `json:"player"`
	} `json:"data"`
	Success bool `json:"success"`
}

type PlayerInfo struct {
	Username string `json:"username"`
	ID       string `json:"id"`
	RawID    string `json:"raw_id"`
}

func GetByNameOrUUID(param string) (*PlayerInfo, error) {
	encodedParam := url.QueryEscape(param)
	targetURL := playerDBURL + encodedParam

	body, err := Get(targetURL, nil)
	if err != nil {
		slog.Error("Failed to fetch player from PlayerDB", "error", err, "param", param)
		return nil, err
	}

	var response PlayerDBResponse
	if err := json.Unmarshal([]byte(body), &response); err != nil {
		slog.Error("Failed to unmarshal PlayerDB response", "error", err)
		return nil, fmt.Errorf("failed to unmarshal response: %w", err)
	}

	if !response.Success {
		return nil, fmt.Errorf("PlayerDB API returned error: %s", response.Message)
	}

	return &response.Data.Player, nil
}
