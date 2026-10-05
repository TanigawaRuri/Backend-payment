package notifier

import (
    "context"
    "fmt"

    "github.com/resend/resend-go/v2"
    "golang.org/x/time/rate"
    "notification-service/internal/event"
)

type ResendNotifier struct {
    client      *resend.Client
    fromAddress string
    limiter     *rate.Limiter
}

func NewResendNotifier(
    client *resend.Client,
    fromAddress string,
) *ResendNotifier {
    return &ResendNotifier{
        client:         client,
        fromAddress:    fromAddress,
        limiter:        rate.NewLimiter(rate.Limit(8), 1),
    }
}

func (n *ResendNotifier) Send(
    ctx context.Context,
    e event.RewardClaimedEvent,
) error {
    if err := n.limiter.Wait(ctx); err != nil {
        return fmt.Errorf("rate limiter: %w", err)
    }
    
    req := &resend.SendEmailRequest{
        From:  "notification@kyungmunkang.com",
        To: []string{
            e.Email,
        },
        Subject: "You've received a reward!",
        Text: fmt.Sprintf(
            "Congratulations!\n\nYou received %.2f reward.",
            e.Amount,
        ),
    }

    _, err := n.client.Emails.SendWithContext(ctx, req)
    if err != nil {
        return fmt.Errorf("send email via resend: %w", err)
    }

    return nil
}