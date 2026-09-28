package com.tanigawa.rewardplatform.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String encodedPassword;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public User(String email, String encodedPassword, String nickname) {
        this.email = email;
        this.encodedPassword = encodedPassword;
        this.nickname = nickname;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void changePassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }  

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }
}