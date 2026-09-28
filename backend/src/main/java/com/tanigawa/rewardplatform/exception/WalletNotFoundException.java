package com.tanigawa.rewardplatform.exception;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(Long walletId) {
        super("Wallet not found with id: " + walletId);
    }

    public WalletNotFoundException(String message) {
        super(message);
    }
}