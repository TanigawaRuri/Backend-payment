package com.tanigawa.rewardplatform.exception;

public class WalletConflictException extends RuntimeException {
    public WalletConflictException(String message) {
        super(message);
    }
}