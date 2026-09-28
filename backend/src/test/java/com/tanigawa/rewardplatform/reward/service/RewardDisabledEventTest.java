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
import com.tanigawa.rewardplatform.reward.entity.RewardQuantityType;
import com.tanigawa.rewardplatform.reward.repository.RewardEventRepository;
import com.tanigawa.rewardplatform.reward.repository.RewardHistoryRepository;
import com.tanigawa.rewardplatform.user.entity.User;
import com.tanigawa.rewardplatform.user.repository.UserRepository;
import com.tanigawa.rewardplatform.wallet.repository.WalletRepository;

//disabled event claim affects to the wallet balance?
@ExtendWith(MockitoExtension.class)
class RewardDisabledEventTest {
    @Mock
    private RewardHistoryRepository rewardHistoryRepository;

    @Mock
    private RewardEventRepository rewardEventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private RewardEventService rewardEventService;

    @Test
    void claimReward_disabledEvent() {
        Long userId = 1L;
        Long eventId = 3L;
        UUID idempotencyKey = UUID.randomUUID();

        RewardHistoryRequest request =
                    new RewardHistoryRequest(eventId, idempotencyKey);

        User user = User.builder()
                    .email("test@test.com")
                    .encodedPassword("encodedPassword")
                    .nickname("tester")
                    .build();

        RewardEvent event = RewardEvent.builder()
            .name("ChristmasEvent")
            .description("Christmas")
            .rewardAmount(1000L)
            .enabled(false)
            .quantityType(RewardQuantityType.UNLIMITED)
            .remainingCount(null)
            .build();
        
        when(rewardHistoryRepository.findByIdempotencyKey(idempotencyKey))
            .thenReturn(Optional.empty());
        
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(user));
        
        when(rewardEventRepository.findById(eventId))
            .thenReturn(Optional.of(event));

        assertThatThrownBy(() ->
                rewardEventService.claimReward(userId, request))
                .isInstanceOf(RuntimeException.class);
        
        verify(walletRepository, never())
            .findByUserId(any());

        verify(rewardHistoryRepository, never())
            .save(any());

        verify(rewardHistoryRepository)
            .findByIdempotencyKey(idempotencyKey);

        verify(userRepository)
            .findById(userId);

        verify(rewardEventRepository)
            .findById(eventId);
        }
}