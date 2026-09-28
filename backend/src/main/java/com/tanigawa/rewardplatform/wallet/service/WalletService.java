package com.tanigawa.rewardplatform.wallet.service;

import com.tanigawa.rewardplatform.exception.WalletNotFoundException;
import com.tanigawa.rewardplatform.wallet.dto.response.WalletResponse;
import com.tanigawa.rewardplatform.wallet.entity.Wallet;
import com.tanigawa.rewardplatform.wallet.repository.WalletRepository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public WalletResponse getWalletBalance(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for user id: " + userId));

        return WalletResponse.from(wallet);
    }
}