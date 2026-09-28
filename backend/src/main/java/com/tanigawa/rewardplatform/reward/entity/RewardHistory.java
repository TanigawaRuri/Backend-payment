package com.tanigawa.rewardplatform.reward.entity;

import com.tanigawa.rewardplatform.user.entity.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "reward_histories",
    indexes = {
        @Index(name = "idx_reward_history_user", columnList = "user_id"),
        @Index(name = "idx_reward_history_event", columnList = "reward_event_id")
    }
)

public class RewardHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_event_id", nullable = false)
    private RewardEvent rewardEvent;

    @Column(nullable = false)
    private Long points;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RewardStatus status;

    @Column(nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime completedAt;

    @Builder
    public RewardHistory(
        User user,
        RewardEvent rewardEvent,
        Long points,
        RewardStatus status,
        UUID idempotencyKey
        ) {
        this.user = user;
        this.rewardEvent = rewardEvent;
        this.points = points;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void approve() {
        if (status != RewardStatus.PENDING) {
            throw new IllegalStateException("Reward already processed.");
        }

        this.status = RewardStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void reject() {
        if (status != RewardStatus.PENDING) {
            throw new IllegalStateException("Reward already processed.");
        }

        this.status = RewardStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }
}