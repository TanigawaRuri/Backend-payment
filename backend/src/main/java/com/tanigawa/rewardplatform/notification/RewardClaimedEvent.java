package com.tanigawa.rewardplatform.notification;

import java.util.UUID;

public record RewardClaimedEvent(
    UUID eventId,
    Long userId,
    String email,
    Long amount,
    String occurredAt
) {}