package com.example.thinkfast.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@NoArgsConstructor
public class SignUpRequest {
    private String username;
    private String password;
    private String birthDate;

    public LocalDate getBirthDateAsLocalDate() {
        return LocalDate.parse(this.birthDate, DateTimeFormatter.ofPattern("yyMMdd"));
    }
} 