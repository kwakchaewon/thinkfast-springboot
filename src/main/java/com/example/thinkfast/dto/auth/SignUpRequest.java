package com.example.thinkfast.dto.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

@Getter
@NoArgsConstructor
public class SignUpRequest {
    private String username;
    private String password;
    private String birthDate;
    private String realUsername; // 실제 유저 이름 (선택적)

    public LocalDate getBirthDateAsLocalDate() {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendValueReduced(ChronoField.YEAR, 2, 2, 1950) // <- 여기서 기준 연도 설정
                .appendPattern("MMdd")
                .toFormatter();

        return LocalDate.parse(this.birthDate, formatter);
    }
} 