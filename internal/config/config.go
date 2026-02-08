package config

import (
	"fmt"
	"os"

	"gopkg.in/yaml.v3"
)

type AppConfig struct {
	Bot      BotConfig      `yaml:"bot"`
	Aystone  AystoneConfig  `yaml:"aystone"`
	Twitch   TwitchConfig   `yaml:"twitch"`
	Database DatabaseConfig `yaml:"database"`
}

type BotConfig struct {
	Token    string   `yaml:"token"`
	Activity string   `yaml:"activity"`
	RolesID  []string `yaml:"rolesId"`
}

type AystoneConfig struct {
	URL   string `yaml:"url"`
	Token string `yaml:"token"`
}

type TwitchConfig struct {
	APIURL       string `yaml:"apiUrl"`
	TokenURL     string `yaml:"tokenUrl"`
	ClientID     string `yaml:"clientId"`
	ClientSecret string `yaml:"clientSecret"`
}

type DatabaseConfig struct {
	Host     string `yaml:"host"`
	Port     string `yaml:"port"`
	Name     string `yaml:"name"`
	Schema   string `yaml:"schema"`
	Username string `yaml:"username"`
	Password string `yaml:"password"`
}

func LoadConfig() (*AppConfig, error) {
	file, err := os.Open("config.yaml")
	if err != nil {
		return nil, fmt.Errorf("failed to open config.yaml: %w", err)
	}
	defer func(file *os.File) {
		_ = file.Close()
	}(file)

	var config AppConfig
	decoder := yaml.NewDecoder(file)
	if err := decoder.Decode(&config); err != nil {
		return nil, fmt.Errorf("failed to decode config.yaml: %w", err)
	}

	return &config, nil
}
