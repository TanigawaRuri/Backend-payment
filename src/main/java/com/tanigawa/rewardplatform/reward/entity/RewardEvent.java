package com.tanigawa.rewardplatform.reward.entity;

import java.time.LocalDateTime;

import com.tanigawa.rewardplatform.exception.RewardEventExhaustedException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "reward_events")
public class RewardEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false)
    private Long rewardAmount;

    @Column(nullable = false)
    private Boolean enabled;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RewardQuantityType quantityType;

    @Column
    private Integer remainingCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public RewardEvent(String name, String description, Long rewardAmount, Boolean enabled, RewardQuantityType quantityType, Integer remainingCount) {
        this.name = name;
        this.description = description;
        this.rewardAmount = rewardAmount;
        this.enabled = enabled;
        this.quantityType = quantityType;
        this.remainingCount = remainingCount;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void enable() {this.enabled = true;}
    public void disable() {this.enabled = false;}
    public void claim() {
        if (quantityType == RewardQuantityType.LIMITED) {
            if (remainingCount == null || remainingCount <= 0) {
                throw new RewardEventExhaustedException(
                        "No remaining count for reward event id=" + this.id);
            }
            this.remainingCount--;
        }
    }
}