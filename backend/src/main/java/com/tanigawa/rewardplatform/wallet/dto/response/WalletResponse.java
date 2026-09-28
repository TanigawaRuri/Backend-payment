package com.tanigawa.rewardplatform.wallet.dto.response;

import com.tanigawa.rewardplatform.wallet.entity.Wallet;

public record WalletResponse(
    Long userId,
    Long balance
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
            wallet.getUser().getId(),
            wallet.getBalance()
        );
    }
}