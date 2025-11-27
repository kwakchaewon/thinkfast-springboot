package com.example.thinkfast.dto.auth;

import com.example.thinkfast.domain.auth.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String username;
    private String realUsername;
    private Role role;
    private LocalDate birthDate;
}

