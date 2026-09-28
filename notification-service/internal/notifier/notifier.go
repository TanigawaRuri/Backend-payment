package notifier

import (
	"context"

	"notification-service/internal/event"
)

type Notifier interface {
	Send(ctx context.Context, e event.RewardClaimedEvent) error
}