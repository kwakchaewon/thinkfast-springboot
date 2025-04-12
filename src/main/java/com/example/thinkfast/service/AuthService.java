package com.example.thinkfast.service;

import com.example.thinkfast.domain.auth.RefreshToken;
import com.example.thinkfast.domain.auth.User;
import com.example.thinkfast.domain.auth.Role;
import com.example.thinkfast.dto.auth.LoginRequest;
import com.example.thinkfast.dto.auth.SignUpRequest;
import com.example.thinkfast.dto.auth.TokenResponse;
import com.example.thinkfast.repository.auth.RefreshTokenRepository;
import com.example.thinkfast.repository.auth.UserRepository;
import com.example.thinkfast.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signUp(SignUpRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.RESPONDER) // 기본 역할은 RESPONDER
                .birthDate(request.getBirthDateAsLocalDate())
                .build();

        userRepository.save(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenProvider.createAccessToken(authentication.getName());
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication.getName());

        // 기존 리프레시 토큰이 있다면 삭제
        refreshTokenRepository.findByUsername(request.getUsername())
                .ifPresent(refreshTokenRepository::delete);

        // 새로운 리프레시 토큰 저장
        RefreshToken newRefreshToken = RefreshToken.builder()
                .username(request.getUsername())
                .token(refreshToken)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(newRefreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public TokenResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }

        String username = jwtTokenProvider.getUsername(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("저장된 리프레시 토큰이 없습니다."));

        if (!storedToken.getUsername().equals(username)) {
            throw new RuntimeException("토큰의 사용자 정보가 일치하지 않습니다.");
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);
        String newAccessToken = jwtTokenProvider.createAccessToken(authentication.getName());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(authentication.getName());

        // 기존 리프레시 토큰 삭제
        refreshTokenRepository.delete(storedToken);

        // 새로운 리프레시 토큰 저장
        RefreshToken newStoredToken = RefreshToken.builder()
                .username(username)
                .token(newRefreshToken)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(newStoredToken);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Transactional
    public void logout(String token) {
        String username = jwtTokenProvider.getUsername(token);
        refreshTokenRepository.findByUsername(username)
                .ifPresent(refreshTokenRepository::delete);
    }
} 