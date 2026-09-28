package com.tanigawa.rewardplatform.exception;

public class RewardDisabledException extends RuntimeException {
    public RewardDisabledException(String message) {
        super(message);
    }
}