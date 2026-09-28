package com.tanigawa.rewardplatform.reward.dto.request;

import com.tanigawa.rewardplatform.reward.entity.RewardQuantityType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RewardEventRequest(
    @NotBlank(message = "Name is required")
    String name,

    String description,

    @NotNull(message = "Reward amount is required")
    @PositiveOrZero(message = "Reward amount must be 0 or greater")
    Long rewardAmount,

    @NotNull(message = "Enabled is required")
    Boolean enabled,

    @NotNull(message = "Quantity type is required")
    RewardQuantityType quantityType,

    @PositiveOrZero(message = "Remaining count must be 0 or greater")
    Integer remainingCount
) {
}