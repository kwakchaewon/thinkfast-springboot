package com.example.thinkfast.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class SignUpRequest {
    private String username;
    private String password;
    private LocalDate birthDate;
} 