package com.example.thinkfast.controller;

import com.example.thinkfast.common.BaseResponse;
import com.example.thinkfast.common.BaseResponseBody;
import com.example.thinkfast.domain.auth.RefreshToken;
import com.example.thinkfast.dto.auth.LoginRequest;
import com.example.thinkfast.dto.auth.SignUpRequest;
import com.example.thinkfast.dto.auth.TokenResponse;
import com.example.thinkfast.security.JwtTokenProvider;
import com.example.thinkfast.service.AuthService;
import com.example.thinkfast.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@BaseResponseBody
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthService authService;

    @PostMapping("/signup")
    public BaseResponse signUp(@RequestBody SignUpRequest request) {
        if (authService.checkDuplicatedUser(request.getUsername())) {
            return BaseResponse.fail("이미 가입된 계정입니다.");
        }
        
        authService.signUp(request);
        return BaseResponse.success();
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * 클라이언트(프론트엔드)가 API 요청을 보냈는데, 서버에서 401 Unauthorized 에러를 반환시
     * 프론트엔드에서는 토큰 만료를 감지해서 자동으로 갱신하도록 구성
     * Vue, React, Axios 등에서 인터셉터를 이용해서 구현
     * @param refreshToken
     * @return
     */
    @PostMapping("/refresh")
    public TokenResponse refreshToken(@RequestHeader("Authorization") String refreshToken) {
        return authService.refreshToken(refreshToken);
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
    }
} 