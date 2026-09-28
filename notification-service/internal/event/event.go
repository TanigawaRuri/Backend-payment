package event

import "time"

type RewardClaimedEvent struct {
	EventID    string    `json:"eventId"`
	UserID     int64     `json:"userId"`
	WalletID   int64     `json:"walletId"`
	Amount     float64   `json:"amount"`
	OccurredAt time.Time `json:"occurredAt"`
}