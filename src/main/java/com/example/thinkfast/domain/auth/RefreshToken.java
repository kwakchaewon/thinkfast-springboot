package com.example.thinkfast.domain.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "REFRESH_TOKENS")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "TOKEN", nullable = false, unique = true)
    private String token;

    @Column(name = "USERNAME", nullable = false)
    private String username;

    @Column(nullable = false, name = "EXPIRES_AT")
    private LocalDateTime expiryDate;

    @Builder
    public RefreshToken(String token, String username, LocalDateTime expiryDate) {
        this.token = token;
        this.username = username;
        this.expiryDate = expiryDate;
    }
}