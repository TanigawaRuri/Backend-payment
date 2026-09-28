package com.tanigawa.rewardplatform.reward.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.tanigawa.rewardplatform.auth.jwt.TokenProvider;
import com.tanigawa.rewardplatform.reward.dto.request.RewardHistoryRequest;
import com.tanigawa.rewardplatform.reward.entity.RewardEvent;
import com.tanigawa.rewardplatform.reward.entity.RewardHistory;
import com.tanigawa.rewardplatform.reward.entity.RewardQuantityType;
import com.tanigawa.rewardplatform.reward.entity.RewardStatus;
import com.tanigawa.rewardplatform.reward.repository.RewardEventRepository;
import com.tanigawa.rewardplatform.reward.repository.RewardHistoryRepository;
import com.tanigawa.rewardplatform.user.entity.User;
import com.tanigawa.rewardplatform.user.repository.UserRepository;
import com.tanigawa.rewardplatform.wallet.entity.Wallet;
import com.tanigawa.rewardplatform.wallet.repository.WalletRepository;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClaimRewardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private RewardEventRepository rewardEventRepository;

    @Autowired
    private RewardHistoryRepository rewardHistoryRepository;

    private User user;
    private RewardEvent event, event2;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = userRepository.save(
                User.builder()
                        .email("claim-" + UUID.randomUUID() + "@test.com")
                        .encodedPassword("encoded-password")
                        .nickname("tester")
                        .build()
        );

        walletRepository.save(new Wallet(user));

        event = rewardEventRepository.save(
                RewardEvent.builder()
                        .name("SIGNUP_" + UUID.randomUUID())
                        .description("integration test event")
                        .rewardAmount(500L)
                        .enabled(true)
                        .quantityType(RewardQuantityType.UNLIMITED) // 기존 테스트는 대부분 무제한으로 가정해도 무방
                        .remainingCount(null)
                        .build()
        );

        event2 = rewardEventRepository.save(
                RewardEvent.builder()
                        .name("SIGNUP_" + UUID.randomUUID())
                        .description("integration test event")
                        .rewardAmount(300L)
                        .enabled(true)
                        .quantityType(RewardQuantityType.UNLIMITED) // 기존 테스트는 대부분 무제한으로 가정해도 무방
                        .remainingCount(null)
                        .build()
        );

        accessToken = tokenProvider.createToken(user.getId(), user.getEmail());
    }

    @Test
    void claimReward_success() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        RewardHistoryRequest request = new RewardHistoryRequest(event.getId(), idempotencyKey);

       mockMvc.perform(post("/api/reward-events/claims")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.points").value(event.getRewardAmount()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        RewardHistory savedHistory = rewardHistoryRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new AssertionError("RewardHistory was not persisted"));

        assertThat(savedHistory.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedHistory.getRewardEvent().getId()).isEqualTo(event.getId());
        assertThat(savedHistory.getPoints()).isEqualTo(event.getRewardAmount());
        assertThat(savedHistory.getStatus()).isEqualTo(RewardStatus.COMPLETED);

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AssertionError("Wallet not found"));

        assertThat(wallet.getBalance()).isEqualTo(event.getRewardAmount());
    }

    @Test
    void claimReward_duplicateIdempotencyKey_returnsExistingHistory() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();

        RewardHistoryRequest request1 = new RewardHistoryRequest(event.getId(), idempotencyKey);
        RewardHistoryRequest request2 = new RewardHistoryRequest(event2.getId(), idempotencyKey);

        mockMvc.perform(post("/api/reward-events/claims")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.points").value(event.getRewardAmount()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        
        mockMvc.perform(post("/api/reward-events/claims")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.points").value(event.getRewardAmount()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        
        RewardHistory savedHistory = rewardHistoryRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new AssertionError("RewardHistory was not persisted"));

        assertThat(savedHistory.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedHistory.getRewardEvent().getId()).isEqualTo(event.getId());
        assertThat(savedHistory.getPoints()).isEqualTo(event.getRewardAmount());
        assertThat(savedHistory.getStatus()).isEqualTo(RewardStatus.COMPLETED);

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AssertionError("Wallet not found"));

        assertThat(wallet.getBalance()).isEqualTo(event.getRewardAmount());
    }

    @Test
    void claimReward_disabledEvent_returnsError() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        RewardHistoryRequest request = new RewardHistoryRequest(event.getId(), idempotencyKey);

        event.disable();
        rewardEventRepository.save(event);

        mockMvc.perform(post("/api/reward-events/claims")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Reward event is disabled"));
        
        Wallet wallet = walletRepository.findByUserId(user.getId())
            .orElseThrow(() -> new AssertionError("Wallet not found"));

        assertThat(rewardHistoryRepository.findByIdempotencyKey(idempotencyKey)).isEmpty();
        assertThat(wallet.getBalance()).isZero();
    }

    @Test
    void claimReward_eventNotFound_returnsError() throws Exception {
        Long nonExistentEventId = 999999L;
        UUID idempotencyKey = UUID.randomUUID();
        RewardHistoryRequest request = new RewardHistoryRequest(nonExistentEventId, idempotencyKey);

        mockMvc.perform(post("/api/reward-events/claims")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("없는 이벤트입니다"));
        
        Wallet wallet = walletRepository.findByUserId(user.getId())
            .orElseThrow(() -> new AssertionError("Wallet not found"));

        assertThat(rewardHistoryRepository.findByIdempotencyKey(idempotencyKey)).isEmpty();
        assertThat(wallet.getBalance()).isZero();
    }
}