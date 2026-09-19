package com.tanigawa.rewardplatform.reward.service;

import java.util.List;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tanigawa.rewardplatform.exception.RewardDisabledException;
import com.tanigawa.rewardplatform.exception.RewardEventExhaustedException;
import com.tanigawa.rewardplatform.exception.RewardEventNotFoundException;
import com.tanigawa.rewardplatform.exception.UserNotFoundException;
import com.tanigawa.rewardplatform.exception.WalletConflictException;
import com.tanigawa.rewardplatform.reward.dto.request.RewardEventRequest;
import com.tanigawa.rewardplatform.reward.dto.request.RewardHistoryRequest;
import com.tanigawa.rewardplatform.reward.dto.response.RewardEventResponse;
import com.tanigawa.rewardplatform.reward.dto.response.RewardHistoryResponse;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RewardEventService {
    private final RewardEventRepository rewardEventRepository;
    private final RewardHistoryRepository rewardHistoryRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    public List<RewardEventResponse> findAllEvents() {
        return rewardEventRepository.findAll()
                .stream()
                .map(RewardEventResponse::from)
                .toList();
    }

    @Transactional
    public RewardEventResponse createEvent(RewardEventRequest request) {
        RewardEvent event = RewardEvent.builder()
            .name(request.name())
            .description(request.description())
            .rewardAmount(request.rewardAmount())
            .enabled(request.enabled())
            .quantityType(request.quantityType())
            .remainingCount(request.remainingCount())
            .build();

        if (request.quantityType() == RewardQuantityType.LIMITED && request.remainingCount() == null) {
            throw new IllegalArgumentException("LIMITED 타입은 remainingCount가 필수입니다.");
}    

        RewardEvent savedEvent = rewardEventRepository.save(event);

        return RewardEventResponse.from(savedEvent);
    }

    @Transactional
    public RewardHistoryResponse claimReward(
        Long userId, RewardHistoryRequest request
    ) {
        
        RewardHistory existing = rewardHistoryRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);

        if (existing != null) {
            return RewardHistoryResponse.from(existing);
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
        //Pessimistic
        //RewardEvent event = rewardEventRepository.findByIdWithPessimisticLock(request.rewardEventId()).orElseThrow(() -> new RewardEventNotFoundException("없는 이벤트입니다"));

        //Atomic
        RewardEvent event = rewardEventRepository.findById(request.rewardEventId()).orElseThrow(() -> new RewardEventNotFoundException("없는 이벤트입니다"));

        if(!event.getEnabled()) {
            throw new RewardDisabledException("Reward event is disabled");
        }

        //Pessimistic
        //event.claim();
        
        //Atomic Update
        int updated = rewardEventRepository.claimOneAtomically(event.getId());
        if (updated == 0) throw new RewardEventExhaustedException("리워드 수량이 모두 소진되었습니다.");
        
        Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();

        RewardHistory history = RewardHistory.builder()
                        .user(user)
                        .rewardEvent(event)
                        .points(event.getRewardAmount())
                        .idempotencyKey(request.idempotencyKey())
                        .status(RewardStatus.PENDING)
                        .build();

        history.approve();

        RewardHistory savedHistory = rewardHistoryRepository.save(history);

        wallet.increaseBalance(event.getRewardAmount());

        try {
            walletRepository.saveAndFlush(wallet);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new WalletConflictException("지갑 잔액이 동시에 변경되어 처리하지 못했습니다. 다시 시도해주세요.");
        }

        return RewardHistoryResponse.from(savedHistory);
    }
}