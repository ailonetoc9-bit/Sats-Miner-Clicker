package com.jose.minerclicker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "rewarded_ad_sessions")
public class RewardedAdSession {
    @Id
    private String id;
    private String telegramId;

    @Column(name = "reward_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private RewardType rewardType;

    @Column(name = "session_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private RewardStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    protected RewardedAdSession() {}

    public RewardedAdSession(String id, String telegramId, RewardType rewardType,
                             LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.telegramId = telegramId;
        this.rewardType = rewardType;
        this.status = RewardStatus.PENDING;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public String getTelegramId() { return telegramId; }
    public RewardType getRewardType() { return rewardType; }
    public RewardStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setStatus(RewardStatus status) { this.status = status; }

    public enum RewardType { DOUBLE, COOL, WITHDRAWAL_TOKEN }
    public enum RewardStatus { PENDING, REWARDED, NON_VALUED, EXPIRED }
}
