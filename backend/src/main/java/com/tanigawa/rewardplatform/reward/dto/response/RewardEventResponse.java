package com.tanigawa.rewardplatform.reward.dto.response;

import com.tanigawa.rewardplatform.reward.entity.RewardEvent;

public record RewardEventResponse (
    Long id,
    String name,
    String description,
    Long rewardAmount,
    Boolean enabled
) {
    public static RewardEventResponse from(RewardEvent event) {
        return new RewardEventResponse(
            event.getId(),
            event.getName(),
            event.getDescription(),
            event.getRewardAmount(),
            event.getEnabled()
        );
    }
}