package com.example.thinkfast.realtime;

import com.example.thinkfast.common.logger.LoggingInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Component
@RequiredArgsConstructor
public class AlarmHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
    @Autowired
    private ObjectMapper objectMapper;
    
    // username 을 키 값으로 웹 소켓 세션들의 집합. (멀티 쓰레드 환경: 같은 유저가 여러 브라우저나 기기에서 접속해도 독립적으로 관리)
    private Map<String, Set<WebSocketSession>> alarmSessions = new ConcurrentHashMap<>();  // userId -> Set<WebSocketSession>

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = (String) session.getAttributes().get("username");

        // userId에 해당하는 세션 리스트가 없다면 새로 생성
        alarmSessions.putIfAbsent(username, ConcurrentHashMap.newKeySet());

        // userId에 해당하는 세션 리스트에 현재 세션 추가
        alarmSessions.get(username).add(session);
        log.info("[WEBSOCKET] CONNECTED {} => sessionId: {}", username, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = (String) session.getAttributes().get("username");

        // 세션을 종료 시, 해당 userId에 대한 세션 리스트에서 제거
        Set<WebSocketSession> sessions = alarmSessions.get(username);
        if (sessions != null) {
            sessions.remove(session);
        }

        log.info("[WEBSOCKET] DISCONNECTED {} => sessionId: {}, reason: {}", username, session.getId(), status.getReason());
    }

    // Redis 로부터 이벤트가 오면 해당 userId에 연결된 모든 세션에 메시지 전송
    public void sendToUser(String userId, Object message) {
        Set<WebSocketSession> sessions = alarmSessions.get(userId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        String json = objectMapper.writeValueAsString(message);
                        session.sendMessage(new TextMessage(json));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
