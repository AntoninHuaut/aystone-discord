package api

import (
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"time"
)

var client = &http.Client{
	Timeout: 30 * time.Second,
}

func Get(url string, headers map[string]string) (string, error) {
	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return "", fmt.Errorf("failed to create request: %w", err)
	}

	for key, value := range headers {
		req.Header.Set(key, value)
	}

	resp, err := client.Do(req)
	if err != nil {
		return "", fmt.Errorf("failed to execute request: %w", err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("failed to read response body: %w", err)
	}

	if resp.StatusCode >= 400 {
		return "", fmt.Errorf("HTTP error %d: %s", resp.StatusCode, string(body))
	}

	return string(body), nil
}

func PostFormData(targetURL string, params map[string]string) (string, error) {
	formData := url.Values{}
	for key, value := range params {
		formData.Set(key, value)
	}

	req, err := http.NewRequest("POST", targetURL, strings.NewReader(formData.Encode()))
	if err != nil {
		return "", fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	resp, err := client.Do(req)
	if err != nil {
		return "", fmt.Errorf("failed to execute request: %w", err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("failed to read response body: %w", err)
	}

	if resp.StatusCode >= 400 {
		return "", fmt.Errorf("HTTP error %d: %s", resp.StatusCode, string(body))
	}

	return string(body), nil
}
