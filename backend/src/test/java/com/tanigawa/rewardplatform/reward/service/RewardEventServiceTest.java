package com.tanigawa.rewardplatform.reward.service;

import com.tanigawa.rewardplatform.reward.dto.request.RewardHistoryRequest;
import com.tanigawa.rewardplatform.reward.dto.response.RewardHistoryResponse;
import com.tanigawa.rewardplatform.reward.entity.RewardEvent;
import com.tanigawa.rewardplatform.reward.entity.RewardHistory;
import com.tanigawa.rewardplatform.reward.entity.RewardQuantityType;
import com.tanigawa.rewardplatform.reward.repository.RewardEventRepository;
import com.tanigawa.rewardplatform.reward.repository.RewardHistoryRepository;

import com.tanigawa.rewardplatform.user.entity.User;
import com.tanigawa.rewardplatform.user.repository.UserRepository;

import com.tanigawa.rewardplatform.wallet.entity.Wallet;
import com.tanigawa.rewardplatform.wallet.repository.WalletRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RewardEventServiceTest {
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
    void claimReward_success() {
        Long userId = 1L;
        Long eventId = 10L;
        UUID idempotencyKey = UUID.randomUUID();

        RewardHistoryRequest request =
                new RewardHistoryRequest(
                        eventId,
                        idempotencyKey
                );

        User user = new User(
                    "test@test.com",
                    "encoded-password",
                    "tester"
                );

        RewardEvent event = RewardEvent.builder()
                .name("name1")
                .description("description1")
                .rewardAmount(500L)
                .enabled(true)
                .quantityType(RewardQuantityType.UNLIMITED)
                .remainingCount(null)
                .build();

                
        Wallet wallet = new Wallet(user);
        wallet.increaseBalance(1000L);

        when(rewardHistoryRepository
                .findByIdempotencyKey(request.idempotencyKey()))
                .thenReturn(Optional.empty());

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(rewardEventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(walletRepository.findByUserId(userId))
                .thenReturn(Optional.of(wallet));

        when(rewardHistoryRepository.save(any(RewardHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RewardHistoryResponse response =
                rewardEventService.claimReward(userId, request);

        assertThat(response)
                .isNotNull();

        assertThat(wallet.getBalance())
                .isEqualTo(1500L);

        verify(rewardHistoryRepository)
                .save(any(RewardHistory.class));
    }
}