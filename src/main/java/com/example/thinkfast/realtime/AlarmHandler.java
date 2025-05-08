package com.example.thinkfast.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Component
@RequiredArgsConstructor
// Websocket 메시지 송신 담당
public class AlarmHandler extends TextWebSocketHandler {
    // 여러 개의 브라우저나 여러 기기에서 동일한 userId 로 접속하기 때문에 각각 세션을 독립적으로 관리
    // 세션 종료 시 세션을 식별하여 세션 제거
    // userId를 key로, 해당 userId에 대한 모든 세션을 관리하는 구조
    @Autowired
    private ObjectMapper objectMapper;
    private Map<String, Set<WebSocketSession>> alarmSessions = new ConcurrentHashMap<>();  // userId -> Set<WebSocketSession>

    // /ws/alarm 으로 웹소켓 연결 시도 시, 호출. 연결된 알람 세션을 저장
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = (String) session.getAttributes().get("username");

        // userId에 해당하는 세션 리스트가 없다면 새로 생성
        alarmSessions.putIfAbsent(username, ConcurrentHashMap.newKeySet());

        // userId에 해당하는 세션 리스트에 현재 세션 추가
        alarmSessions.get(username).add(session);
        System.out.println("User " + username + " WebSocket 연결됨");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = (String) session.getAttributes().get("username");

        // 세션을 종료하면 해당 userId에 대한 세션 리스트에서 제거
        Set<WebSocketSession> sessions = alarmSessions.get(username);
        if (sessions != null) {
            sessions.remove(session);
        }

        System.out.println("User " + username + " WebSocket 연결 해제됨");
    }

    // Redis로부터 이벤트가 오면 해당 이벤트를 모든 세션에 전송
    public void sendToUser(String userId, Object message) {
        // 해당 userId에 연결된 모든 세션에 메시지 전송
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
