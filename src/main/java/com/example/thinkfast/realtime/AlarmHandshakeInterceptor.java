package com.example.thinkfast.realtime;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

public class AlarmHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // 1. 요청 URI를 가져오기 ex) /alarm/user1
        URI uri = request.getURI();

        // 2. /alarm/user1 구조일 경우 username 키로 user1 저장 후 웹 소켓 세션에 바인딩
        String[] parts = uri.getPath().split("/");
        if (parts.length >= 3) {
            attributes.put("username", parts[2]);
        }

        // 3. Handshake를 허용
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}