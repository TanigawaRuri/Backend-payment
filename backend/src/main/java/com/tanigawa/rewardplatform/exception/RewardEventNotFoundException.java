package com.tanigawa.rewardplatform.exception;

public class RewardEventNotFoundException extends RuntimeException {

    public RewardEventNotFoundException(Long rewardEventId) {
        super("Reward event not found with id: " + rewardEventId);
    }

    public RewardEventNotFoundException(String message) {
        super(message);
    }
}