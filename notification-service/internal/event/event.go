package event

import "time"

type RewardClaimedEvent struct {
	EventID    string    `json:"eventId"`
	UserID     int64     `json:"userId"`
	Email	   string	 `json: "email"`
	Amount     float64   `json:"amount"`
	OccurredAt time.Time `json:"occurredAt"`
}