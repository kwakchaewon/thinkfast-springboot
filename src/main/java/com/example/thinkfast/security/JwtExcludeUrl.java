package com.example.thinkfast.security;

import lombok.Getter;

@Getter
public enum JwtExcludeUrl {
    AUTH_REFRESH("/auth/refresh"),
    AUTH_LOGOUT("/auth/logout");

    private final String url;

    JwtExcludeUrl(String url) {
        this.url = url;
    }

    // Java 1.8 스타일로 모든 URL을 배열로 반환
    public static String[] getUrls() {
        JwtExcludeUrl[] values = JwtExcludeUrl.values();
        String[] urls = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            urls[i] = values[i].getUrl();
        }
        return urls;
    }
}
