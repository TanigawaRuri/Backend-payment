package com.tanigawa.rewardplatform.exception;

public class RewardHistoryNotFoundException extends RuntimeException {

    public RewardHistoryNotFoundException(Long historyId) {
        super("Reward history not found with id: " + historyId);
    }

    public RewardHistoryNotFoundException(String message) {
        super(message);
    }
}