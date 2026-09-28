package consumer

import (
	"context"
	"encoding/json"
	"log/slog"
	"time"

	"github.com/resend/resend-go/v2"
	"github.com/segmentio/kafka-go"

	"notification-service/internal/event"
	"notification-service/internal/idempotency"
	"notification-service/internal/notifier"
)

type Consumer struct {
	reader      *kafka.Reader
	notifier    notifier.Notifier
	idempotency idempotency.Store
	workerCount int
	logger      *slog.Logger
	resendClient *resend.Client
	fromAddress  string
}

func New(
	brokers []string,
	topic, groupID string,
	workerCount int,
	n notifier.Notifier,
	idem idempotency.Store,
	logger *slog.Logger,
) *Consumer {
	reader := kafka.NewReader(kafka.ReaderConfig{
		Brokers:     brokers,
		Topic:       topic,
		GroupID:     groupID,
		MinBytes:    1,
		MaxBytes:    10e6,
		StartOffset: kafka.FirstOffset,
	})
	return &Consumer{
		reader:      reader,
		notifier:    n,
		idempotency: idem,
		workerCount: workerCount,
		logger:      logger,
	}
}

func (c *Consumer) Run(ctx context.Context) error {
	msgCh := make(chan kafka.Message)
	done := make(chan struct{})

	for i := 0; i < c.workerCount; i++ {
		go c.worker(ctx, i, msgCh, done)
	}

	defer func() {
		close(msgCh)
		for i := 0; i < c.workerCount; i++ {
			<-done 
		}
	}()

	for {
		msg, err := c.reader.FetchMessage(ctx)
		if err != nil {
			if ctx.Err() != nil {
				return c.reader.Close()
			}
			c.logger.Error("fetch message failed", "error", err)
			continue
		}

		select {
		case msgCh <- msg:
		case <-ctx.Done():
			return c.reader.Close()
		}
	}
}

func (c *Consumer) worker(ctx context.Context, id int, msgCh <-chan kafka.Message, done chan<- struct{}) {
	defer func() { done <- struct{}{} }()

	for msg := range msgCh {
		if err := c.handle(ctx, msg); err != nil {
			c.logger.Error("handle failed, offset not committed, will redeliver",
				"worker", id, "error", err)
			continue
		}
		if err := c.reader.CommitMessages(ctx, msg); err != nil {
			c.logger.Error("commit failed", "worker", id, "error", err)
		}
	}
}

func (c *Consumer) handle(ctx context.Context, msg kafka.Message) error {
	var e event.RewardClaimedEvent
	if err := json.Unmarshal(msg.Value, &e); err != nil {
		c.logger.Error("unmarshal failed, dropping message", "error", err)
		return nil
	}

	if c.idempotency.SeenBefore(e.EventID) {
		c.logger.Info("duplicate event, skipping", "eventId", e.EventID)
		return nil
	}

	return c.sendWithRetry(ctx, e)
}

func (c *Consumer) sendWithRetry(ctx context.Context, e event.RewardClaimedEvent) error {
	backoff := 200 * time.Millisecond
	var lastErr error

	for attempt := 1; attempt <= 3; attempt++ {
		if err := c.notifier.Send(ctx, e); err != nil {
			lastErr = err
			c.logger.Warn("notifier send failed, retrying",
				"eventId", e.EventID, "attempt", attempt, "error", err)
			select {
			case <-time.After(backoff):
			case <-ctx.Done():
				return ctx.Err()
			}
			backoff *= 2
			continue
		}
		return nil
	}
	return lastErr
}