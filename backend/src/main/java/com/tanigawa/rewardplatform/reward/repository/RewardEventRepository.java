package com.tanigawa.rewardplatform.reward.repository;

import com.tanigawa.rewardplatform.reward.entity.RewardEvent;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RewardEventRepository extends JpaRepository<RewardEvent, Long> {
    @Modifying
    @Query("UPDATE RewardEvent r SET r.remainingCount = r.remainingCount - 1 " +
        "WHERE r.id = :id AND r.remainingCount > 0")
    int claimOneAtomically(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
    @Query("SELECT r FROM RewardEvent r WHERE r.id = :id")
    Optional<RewardEvent> findByIdWithPessimisticLock(@Param("id") Long id);
    Optional<RewardEvent> findByName(String name);
}
