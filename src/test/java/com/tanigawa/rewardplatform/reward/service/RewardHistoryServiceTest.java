package com.tanigawa.rewardplatform.reward.service;

import com.tanigawa.rewardplatform.user.entity.User;
import com.tanigawa.rewardplatform.reward.entity.RewardEvent;
import com.tanigawa.rewardplatform.reward.entity.RewardStatus;
import com.tanigawa.rewardplatform.reward.dto.request.RewardHistoryRequest;
import com.tanigawa.rewardplatform.reward.dto.response.RewardHistoryResponse;
import com.tanigawa.rewardplatform.reward.entity.RewardHistory;
import com.tanigawa.rewardplatform.reward.entity.RewardQuantityType;
import com.tanigawa.rewardplatform.reward.repository.RewardEventRepository;
import com.tanigawa.rewardplatform.reward.repository.RewardHistoryRepository;
import com.tanigawa.rewardplatform.user.repository.UserRepository;
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
class RewardHistoryServiceTest {
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
    void claimReward_duplicateRequest_returnsExistingHistory() {
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

        Long userId = 2L;
        Long eventId = 3L;
        UUID idempotencyKey = UUID.randomUUID();

        RewardHistoryRequest request =
            new RewardHistoryRequest(eventId, idempotencyKey);

        RewardHistory existingHistory = RewardHistory.builder()
                .user(user)
                .rewardEvent(event)
                .points(500L)
                .status(RewardStatus.COMPLETED)
                .idempotencyKey(idempotencyKey)
                .build();

        when(rewardHistoryRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingHistory));

        RewardHistoryResponse response =
                rewardEventService.claimReward(userId, request);

        assertThat(response).isNotNull();

        verify(rewardHistoryRepository)
                .findByIdempotencyKey(idempotencyKey);

        verify(userRepository, never())
                .findById(any());

        verify(rewardEventRepository, never())
                .findById(any());

        verify(walletRepository, never())
                .findByUserId(any());

        verify(rewardHistoryRepository, never())
                .save(any(RewardHistory.class));
    }
}