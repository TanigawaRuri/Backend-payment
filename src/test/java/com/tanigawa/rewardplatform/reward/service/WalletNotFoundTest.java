package com.tanigawa.rewardplatform.reward.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tanigawa.rewardplatform.reward.dto.request.RewardHistoryRequest;
import com.tanigawa.rewardplatform.reward.entity.RewardEvent;
import com.tanigawa.rewardplatform.reward.entity.RewardHistory;
import com.tanigawa.rewardplatform.reward.entity.RewardQuantityType;
import com.tanigawa.rewardplatform.reward.repository.RewardEventRepository;
import com.tanigawa.rewardplatform.reward.repository.RewardHistoryRepository;
import com.tanigawa.rewardplatform.user.entity.User;
import com.tanigawa.rewardplatform.user.repository.UserRepository;
import com.tanigawa.rewardplatform.wallet.repository.WalletRepository;

//for the rewardClaims, wallet didn't find

@ExtendWith(MockitoExtension.class)
class WalletNotFoundTest {
    @Mock
    private RewardHistoryRepository rewardHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RewardEventRepository rewardEventRepository;

    @InjectMocks
    private RewardEventService rewardEventService;

    @Mock
    private WalletRepository walletRepository;


    @Test
        void claimReward_walletNotFound() {
            Long userId = 1L;
            Long eventId = 10L;
            UUID idempotencyKey = UUID.randomUUID();

            RewardHistoryRequest request =
                    new RewardHistoryRequest(eventId, idempotencyKey);

            User user = User.builder()
                    .email("test@test.com")
                    .encodedPassword("encodedPassword")
                    .nickname("tester")
                    .build();

            RewardEvent event = RewardEvent.builder()
                .name("Welcome")
                .description("First login reward")
                .rewardAmount(500L)
                .enabled(true)
                .quantityType(RewardQuantityType.UNLIMITED) // 기존 테스트는 대부분 무제한으로 가정해도 무방
                .remainingCount(null)
                .build();

            when(rewardHistoryRepository.findByIdempotencyKey(idempotencyKey))
                    .thenReturn(Optional.empty());

            when(userRepository.findById(userId))
                    .thenReturn(Optional.of(user));

            when(rewardEventRepository.findById(eventId))
                    .thenReturn(Optional.of(event));

            when(walletRepository.findByUserId(userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    rewardEventService.claimReward(userId, request))
                    .isInstanceOf(RuntimeException.class);

            verify(rewardHistoryRepository)
                    .findByIdempotencyKey(idempotencyKey);

            verify(userRepository)
                    .findById(userId);

            verify(rewardEventRepository)
                    .findById(eventId);

            verify(walletRepository)
                    .findByUserId(userId);

            verify(rewardHistoryRepository, never())
                    .save(any(RewardHistory.class));
        }
}