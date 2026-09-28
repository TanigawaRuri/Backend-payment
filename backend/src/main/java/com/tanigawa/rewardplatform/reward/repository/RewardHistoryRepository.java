package com.tanigawa.rewardplatform.reward.repository;

import com.tanigawa.rewardplatform.reward.entity.RewardHistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface RewardHistoryRepository extends JpaRepository<RewardHistory, Long>{
    Optional<RewardHistory> findByIdempotencyKey(UUID idempotencyKey);
    List<RewardHistory> findByUserId(Long userId);
}