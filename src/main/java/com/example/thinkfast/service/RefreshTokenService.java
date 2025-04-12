package com.example.thinkfast.service;

import com.example.thinkfast.domain.auth.RefreshToken;
import com.example.thinkfast.repository.auth.RefreshTokenRepository;
import com.example.thinkfast.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public RefreshToken createRefreshToken(String username) {
        RefreshToken refreshToken = refreshTokenRepository.findByUsername(username)
                .map(existingToken -> {
                    existingToken.setExpiryDate(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenValidityInSeconds()));
                    return existingToken;
                })
                .orElseGet(() -> {
                    String token = jwtTokenProvider.createRefreshToken(username);
                    return RefreshToken.builder()
                            .username(username)
                            .token(token)
                            .expiryDate(LocalDateTime.now().plusSeconds(jwtTokenProvider.getRefreshTokenValidityInSeconds()))
                            .build();
                });

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Transactional
    public int deleteByUsername(String username) {
        return refreshTokenRepository.deleteByUsername(username);
    }
} 