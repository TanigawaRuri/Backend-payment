package com.tanigawa.rewardplatform.user.service;

import com.tanigawa.rewardplatform.exception.UserNotFoundException;
import com.tanigawa.rewardplatform.user.dto.request.RegisterRequest;
import com.tanigawa.rewardplatform.user.dto.response.UserResponse;
import com.tanigawa.rewardplatform.user.entity.User;
import com.tanigawa.rewardplatform.user.repository.UserRepository;
import com.tanigawa.rewardplatform.wallet.entity.Wallet;
import com.tanigawa.rewardplatform.wallet.repository.WalletRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = new User(
            request.email(),
            encodedPassword,
            request.nickname()
        );

        User savedUser = userRepository.save(user);

        Wallet wallet = new Wallet(savedUser);
        walletRepository.save(wallet);

        return new UserResponse(
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getNickname()
        );
    }

    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));
        return new UserResponse(user.getId(), user.getEmail(), user.getNickname());
    }
}

