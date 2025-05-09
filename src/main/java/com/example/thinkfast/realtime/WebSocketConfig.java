package com.example.thinkfast.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
// 웹 소켓 설정 커스터마이징. 웹 소켓 엔드 포인트 등록.
public class WebSocketConfig implements WebSocketConfigurer {
    private final AlarmHandler alarmHandler;

    // 웹소켓 핸들러를 등록하는 메서드
    // 웹소켓 연결을 /alarm 경로로 받고 연결시 alarmHandler 가 처리
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(alarmHandler, "/alarm/{username}")
                .addInterceptors(new AlarmHandshakeInterceptor()) // 인터셉터 등록
                .setAllowedOrigins("*"); // 개발 단계에서는 허용
    }
}
