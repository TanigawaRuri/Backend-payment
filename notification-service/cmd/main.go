package main

import (
	"context"
	"log"
	"log/slog"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/joho/godotenv"
	"github.com/resend/resend-go/v2"

	"notification-service/internal/config"
	"notification-service/internal/consumer"
	"notification-service/internal/idempotency"
	"notification-service/internal/notifier"
)

func main() {
	if err := godotenv.Load(); err != nil {
        log.Println("no .env file found, relying on real env vars")
    }

	cfg, err := config.Load()
	if err != nil {
		log.Fatal(err)
	}

	logger := slog.New(slog.NewJSONHandler(os.Stdout, nil))

	resendClient := resend.NewClient(cfg.ResendAPIKey)

	n := notifier.NewResendNotifier(
		resendClient,
		"onboarding@resend.dev",
	)

	idem := idempotency.NewInMemoryStore(1 * time.Hour)

	c := consumer.New(cfg.KafkaBrokers,
		cfg.KafkaTopic,
		cfg.GroupID,
		cfg.WorkerCount,
		n,
		idem,
		logger)
	
	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	logger.Info("notification-service starting",
		"topic", cfg.KafkaTopic, "groupId", cfg.GroupID, "workers", cfg.WorkerCount)

	if err := c.Run(ctx); err != nil {
		logger.Error("consumer stopped with error", "error", err)
		os.Exit(1)
	}
	logger.Info("notification-service stopped gracefully")
}