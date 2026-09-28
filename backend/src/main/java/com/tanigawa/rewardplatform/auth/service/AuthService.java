package com.tanigawa.rewardplatform.auth.service;

import com.tanigawa.rewardplatform.auth.dto.request.LoginRequest;
import com.tanigawa.rewardplatform.auth.dto.response.TokenResponse;
import com.tanigawa.rewardplatform.auth.jwt.TokenProvider;
import com.tanigawa.rewardplatform.exception.WrongEmailOrPasswordException;
import com.tanigawa.rewardplatform.user.entity.User;
import com.tanigawa.rewardplatform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new WrongEmailOrPasswordException("이메일 또는 비밀번호가 올바르지 않습니다."));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getEncodedPassword())) {
            throw new WrongEmailOrPasswordException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = tokenProvider.createToken(user.getId(), user.getEmail());
        return new TokenResponse(token);
    }
}