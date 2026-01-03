package com.example.thinkfast.security;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import com.example.thinkfast.common.config.WebClientLoggingConfig;

import java.time.Duration;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${gemini.timeout-seconds:30}")
    private int timeoutSeconds;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors().and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                .antMatchers("/public/**").permitAll()
                .antMatchers("/test/**").permitAll()
                .antMatchers("/auth/**").permitAll()
                .antMatchers("/survey/*/questions").permitAll()
                .antMatchers("/survey/*/responses").permitAll()
                .antMatchers("/survey/*").permitAll()
                .antMatchers("/survey/*/summary").permitAll()  // 추가
                .antMatchers("/survey/*/questions/**").permitAll()  // wordcloud, insight, statistics 등도 필요하면 추가
                .antMatchers("/swagger-ui/**", "/swagger-ui.html", "/swagger-resources/**", "/v3/api-docs/**", "/v2/api-docs", "/webjars/**").permitAll()
                .antMatchers("/admin/**").hasRole("ADMIN")
                .antMatchers("/creator/**").hasRole("CREATOR")
                .antMatchers("/alarm/**").permitAll()
                .antMatchers("/notification").permitAll()
                .antMatchers("/notification/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    @Primary
    public WebClient webClient(WebClientLoggingConfig loggingConfig) {
        log.info("SecurityConfig에서 WebClient 빈 생성 시작 - timeout: {}초", timeoutSeconds);
        
        try {
            HttpClient httpClient = HttpClient.create()
                    .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutSeconds * 1000);

            WebClient webClient = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                    .filter(loggingConfig.loggingFilter()) // 로깅 필터 추가
                    .build();
            
            log.info("SecurityConfig에서 WebClient 빈 생성 완료 (ReactorClientHttpConnector 사용)");
            return webClient;
        } catch (NoClassDefFoundError | Exception e) {
            log.warn("ReactorClientHttpConnector를 사용할 수 없습니다. 기본 WebClient를 생성합니다. 오류: {}", e.getMessage());
            // Fallback: 기본 WebClient 생성 (Reactor 없이)
            WebClient webClient = WebClient.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                    .filter(loggingConfig.loggingFilter()) // 로깅 필터 추가
                    .build();
            log.info("SecurityConfig에서 기본 WebClient 빈 생성 완료");
            return webClient;
        }
    }
} 