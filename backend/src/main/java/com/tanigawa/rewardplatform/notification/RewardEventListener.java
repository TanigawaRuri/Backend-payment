package com.tanigawa.rewardplatform.notification;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RewardEventListener {
    private final PaymentEventProducer producer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRewardClaimed(RewardClaimedEvent event) {
        producer.publish(event);
    }
}