package com.tanigawa.rewardplatform.reward.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tanigawa.rewardplatform.exception.RewardHistoryNotFoundException;
import com.tanigawa.rewardplatform.reward.dto.response.RewardHistoryResponse;
import com.tanigawa.rewardplatform.reward.entity.RewardHistory;
import com.tanigawa.rewardplatform.reward.repository.RewardHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RewardHistoryService {
    private final RewardHistoryRepository rewardHistoryRepository;

    @Transactional(readOnly = true)
    public RewardHistoryResponse getHistory(Long id) {
        RewardHistory history = rewardHistoryRepository.findById(id)
                .orElseThrow(() -> new RewardHistoryNotFoundException(id));

    return RewardHistoryResponse.from(history);
}

    @Transactional(readOnly = true)
    public List<RewardHistoryResponse> getUserHistories(Long userId) {
        List<RewardHistory> histories = rewardHistoryRepository.findByUserId(userId);
        return histories.stream()
                .map(RewardHistoryResponse::from)
                .toList();
    }
}
