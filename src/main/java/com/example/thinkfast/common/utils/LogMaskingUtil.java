package com.example.thinkfast.common.utils;

/**
 * 로그에서 민감 정보를 마스킹하는 유틸리티
 */
public class LogMaskingUtil {

    /**
     * 비밀번호, API 키, 토큰 등 민감 정보 마스킹
     */
    public static String maskSensitiveInfo(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // API 키 마스킹
        text = text.replaceAll("(?i)(api[_-]?key|apikey)=[^&\\s]*", "$1=***");
        
        // 토큰 마스킹
        text = text.replaceAll("(?i)(token|bearer|authorization):\\s*[^\\s]*", "$1:***");
        
        // 비밀번호 마스킹
        text = text.replaceAll("(?i)(password|pwd|passwd|secret)=[^&\\s]*", "$1=***");
        
        // 이메일 주소 마스킹 (도메인 제외)
        text = text.replaceAll("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})", 
                maskEmail("$1") + "@$2");
        
        // 전화번호 마스킹
        text = text.replaceAll("(\\d{3})-(\\d{3,4})-(\\d{4})", "$1-****-$3");
        text = text.replaceAll("(\\d{3})(\\d{3,4})(\\d{4})", "$1****$3");
        
        // 주민등록번호 마스킹
        text = text.replaceAll("(\\d{6})-?(\\d{7})", "$1-*******");
        
        // 신용카드 번호 마스킹
        text = text.replaceAll("(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{4})[\\s-]?(\\d{4})", 
                "$1-****-****-$4");
        
        return text;
    }

    /**
     * 이메일 주소 마스킹 (사용자명 부분만)
     */
    private static String maskEmail(String email) {
        if (email == null || email.length() <= 2) {
            return "***";
        }
        if (email.length() <= 4) {
            return email.charAt(0) + "***";
        }
        return email.substring(0, 2) + "***" + email.charAt(email.length() - 1);
    }

    /**
     * JSON 문자열에서 민감 필드 마스킹
     */
    public static String maskSensitiveJson(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        // JSON 필드 마스킹
        json = json.replaceAll("(?i)\"(password|pwd|passwd|secret|api[_-]?key|token|bearer|authorization)\"\\s*:\\s*\"[^\"]*\"", 
                "\"$1\":\"***\"");
        json = json.replaceAll("(?i)\"(password|pwd|passwd|secret|api[_-]?key|token|bearer|authorization)\"\\s*:\\s*[^,}\\]]+", 
                "\"$1\":\"***\"");

        return maskSensitiveInfo(json);
    }

    /**
     * URL에서 민감 정보 마스킹
     */
    public static String maskSensitiveUrl(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        // 쿼리 파라미터 마스킹
        url = url.replaceAll("(?i)([?&])(api[_-]?key|token|password|secret)=[^&]*", "$1$2=***");
        
        // 헤더 정보가 포함된 경우 마스킹
        url = url.replaceAll("(?i)(X-goog-api-key|Authorization):[^\\s]*", "$1:***");

        return url;
    }
}

