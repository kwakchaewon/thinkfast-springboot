package com.example.thinkfast.controller;

import com.example.thinkfast.common.aop.BaseResponse;
import com.example.thinkfast.common.aop.BaseResponseBody;
import com.example.thinkfast.common.aop.ResponseMessage;
import com.example.thinkfast.dto.auth.LoginRequest;
import com.example.thinkfast.dto.auth.RefreshTokenRequest;
import com.example.thinkfast.dto.auth.SignUpRequest;
import com.example.thinkfast.dto.auth.TokenResponse;
import com.example.thinkfast.dto.auth.UpdateProfileRequest;
import com.example.thinkfast.dto.auth.UserProfileResponse;
import com.example.thinkfast.security.UserDetailImpl;
import com.example.thinkfast.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@BaseResponseBody
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "인증 관리", description = "회원가입, 로그인, 토큰 갱신 및 프로필 관리 API")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @PostMapping("/signup")
    public BaseResponse signUp(@RequestBody SignUpRequest request) {
        if (authService.checkDuplicatedUser(request.getUsername())) {
            return BaseResponse.fail(ResponseMessage.ACCOUNT_ALREADY_EXISTS);
        }
        
        authService.signUp(request);
        return BaseResponse.success();
    }

    @Operation(summary = "로그인", description = "사용자 인증 후 JWT 토큰을 발급받습니다.")
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
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
    @PostMapping("/refresh")
    public TokenResponse refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return authService.refreshToken(refreshTokenRequest.getRefreshToken());
    }

    @Operation(summary = "로그아웃", description = "리프레시 토큰을 무효화하여 로그아웃합니다.")
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public void logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
    }

    @Operation(summary = "프로필 수정", description = "현재 사용자의 프로필 정보를 수정합니다.")
    @PutMapping("/profile")
    @PreAuthorize("hasRole('CREATOR') or hasRole('RESPONDER')")
    @SecurityRequirement(name = "bearerAuth")
    public BaseResponse updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetailImpl userDetail
    ) {
        authService.updateProfile(userDetail.getUsername(), request);
        return BaseResponse.success();
    }

    @Operation(summary = "내 프로필 조회", description = "현재 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/me")
    @PreAuthorize("hasRole('CREATOR') or hasRole('RESPONDER')")
    @SecurityRequirement(name = "bearerAuth")
    public UserProfileResponse getMyProfile(
            @AuthenticationPrincipal UserDetailImpl userDetail
    ) {
        return authService.getUserProfile(userDetail.getUsername());
    }
} 