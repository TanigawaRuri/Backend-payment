package config

import (
	"errors"
	"os"
	"strconv"
	"strings"
)

type Config struct {
	KafkaBrokers []string
	KafkaTopic   string
	GroupID      string
	WorkerCount  int
	LogLevel     string
	ResendAPIKey string
}

func Load() (Config, error) {
	cfg := Config{
		KafkaBrokers: strings.Split(getEnv("KAFKA_BROKERS", "kafka:9092"), ","),
		KafkaTopic:   getEnv("KAFKA_TOPIC", "payment-events"),
		GroupID:      getEnv("KAFKA_GROUP_ID", "notification-service"),
		WorkerCount:  getEnvInt("WORKER_COUNT", 4),
		LogLevel:     getEnv("LOG_LEVEL", "info"),
		ResendAPIKey: os.Getenv("RESEND_API_KEY"),
	}

	if cfg.ResendAPIKey == "" {
		return Config{}, errors.New("RESEND_API_KEY is required but not set")
	}

	return cfg, nil
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func getEnvInt(key string, fallback int) int {
	v := os.Getenv(key)
	if v == "" {
		return fallback
	}
	n, err := strconv.Atoi(v)
	if err != nil {
		return fallback
	}
	return n
}