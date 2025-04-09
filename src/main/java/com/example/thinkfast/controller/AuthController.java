package com.example.thinkfast.controller;

import com.example.thinkfast.domain.RefreshToken;
import com.example.thinkfast.dto.LoginRequest;
import com.example.thinkfast.dto.TokenResponse;
import com.example.thinkfast.security.JwtTokenProvider;
import com.example.thinkfast.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = jwtTokenProvider.createAccessToken(loginRequest.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(loginRequest.getUsername());

        return ResponseEntity.ok(TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .build());
    }

    /**
     * 클라이언트(프론트엔드)가 API 요청을 보냈는데, 서버에서 401 Unauthorized 에러를 반환시
     * 프론트엔드에서는 토큰 만료를 감지해서 자동으로 갱신하도록 구성
     * Vue, React, Axios 등에서 인터셉터를 이용해서 구현
     * @param refreshToken
     * @return
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@RequestParam String refreshToken) {
        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUsername)
                .map(username -> {
                    String accessToken = jwtTokenProvider.createAccessToken(username);
                    return ResponseEntity.ok(TokenResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .build());
                })
                .orElseThrow(() -> new RuntimeException("Refresh token not found in database!"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String username) {
        refreshTokenService.deleteByUsername(username);
        return ResponseEntity.ok().build();
    }
} 