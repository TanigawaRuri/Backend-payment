package com.tanigawa.rewardplatform.reward.dto.request;

import java.util.UUID;

public record RewardHistoryRequest(
    Long rewardEventId,
    UUID idempotencyKey
) {
}