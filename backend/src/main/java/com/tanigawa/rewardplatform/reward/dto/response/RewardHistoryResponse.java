package com.tanigawa.rewardplatform.reward.dto.response;

import com.tanigawa.rewardplatform.reward.entity.RewardStatus;
import com.tanigawa.rewardplatform.reward.entity.RewardHistory;

public record RewardHistoryResponse (
    Long id,
    Long userId,
    Long points,
    RewardStatus status
) {
    public static RewardHistoryResponse from(RewardHistory history) {
        return new RewardHistoryResponse(
            history.getId(),
            history.getUser().getId(),
            history.getPoints(),
            history.getStatus()
        );
    }
}