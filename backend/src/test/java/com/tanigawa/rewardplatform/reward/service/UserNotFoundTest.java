package com.tanigawa.rewardplatform.reward.service;

import com.tanigawa.rewardplatform.user.repository.UserRepository;
import com.tanigawa.rewardplatform.wallet.repository.WalletRepository;
import com.tanigawa.rewardplatform.reward.dto.request.RewardHistoryRequest;
import com.tanigawa.rewardplatform.reward.repository.RewardEventRepository;
import com.tanigawa.rewardplatform.reward.repository.RewardHistoryRepository;

import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

//test : user not found for reward event

@ExtendWith(MockitoExtension.class)
class UserNotFoundTest {
    @Mock
    private RewardEventRepository rewardEventRepository;

    @Mock
    private RewardHistoryRepository rewardHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private RewardEventService rewardEventService;

    @Test
    void claimReward_userNotFound() {
        Long userId = 2L;
        Long eventId = 3L;
        UUID idempotencyKey = UUID.randomUUID();

        RewardHistoryRequest request =
            new RewardHistoryRequest(eventId, idempotencyKey);

        when(rewardHistoryRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());
        
        assertThatThrownBy(() ->
                rewardEventService.claimReward(userId, request))
                .isInstanceOf(RuntimeException.class);
        
        verify(walletRepository, never()).findByUserId(any());
    }
}