-- 설문 테이블
CREATE TABLE IF NOT EXISTS SURVEYS (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    CREATOR_ID BIGINT NOT NULL,
    TITLE VARCHAR(255) NOT NULL,
    DESCRIPTION TEXT,
    START_TIME DATETIME NOT NULL,
    END_TIME DATETIME NOT NULL,
    IS_ACTIVE BOOLEAN DEFAULT TRUE,
    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
--     FOREIGN KEY (CREATOR_ID) REFERENCES USERS(ID) ON DELETE CASCADE
);

-- 질문 테이블
CREATE TABLE IF NOT EXISTS QUESTIONS (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    SURVEY_ID BIGINT NOT NULL,
    TYPE ENUM('MULTIPLE_CHOICE', 'SUBJECTIVE', 'SCALE') NOT NULL,
    CONTENT TEXT NOT NULL,
    ORDER_INDEX INT NOT NULL
--     FOREIGN KEY (SURVEY_ID) REFERENCES SURVEYS(ID) ON DELETE CASCADE
);

-- 선택지 테이블
CREATE TABLE IF NOT EXISTS OPTIONS (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    QUESTION_ID BIGINT NOT NULL,
    CONTENT VARCHAR(500) NOT NULL
--     FOREIGN KEY (QUESTION_ID) REFERENCES QUESTIONS(ID) ON DELETE CASCADE
);

-- 응답 테이블
CREATE TABLE IF NOT EXISTS RESPONSES (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    SURVEY_ID BIGINT NOT NULL,
    USER_ID BIGINT NOT NULL,
    SUBMITTED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--     FOREIGN KEY (SURVEY_ID) REFERENCES SURVEYS(ID) ON DELETE CASCADE,
--     FOREIGN KEY (USER_ID) REFERENCES USERS(ID) ON DELETE CASCADE
);

-- 답변 테이블
CREATE TABLE IF NOT EXISTS ANSWERS (
   ID BIGINT AUTO_INCREMENT PRIMARY KEY,
   RESPONSE_ID BIGINT NOT NULL,
   QUESTION_ID BIGINT NOT NULL,
   OPTION_ID BIGINT,
   SUBJECTIVE_ANSWER TEXT,
   SCALE_VALUE INT
--     FOREIGN KEY (RESPONSE_ID) REFERENCES RESPONSES(ID) ON DELETE CASCADE,
--     FOREIGN KEY (QUESTION_ID) REFERENCES QUESTIONS(ID) ON DELETE CASCADE,
--     FOREIGN KEY (OPTION_ID) REFERENCES OPTIONS(ID) ON DELETE SET NULL
);

-- 알림 테이블
CREATE TABLE IF NOT EXISTS NOTIFICATIONS (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    USER_ID BIGINT NOT NULL,
    TYPE ENUM('EMAIL', 'IN_APP') NOT NULL,
    CONTENT TEXT NOT NULL,
    SENT_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    RELATED_SURVEY_ID BIGINT
--     FOREIGN KEY (USER_ID) REFERENCES USERS(ID) ON DELETE CASCADE,
--     FOREIGN KEY (RELATED_SURVEY_ID) REFERENCES SURVEYS(ID) ON DELETE SET NULL
);

-- 로그 테이블
CREATE TABLE IF NOT EXISTS SURVEY_LOGS (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    SURVEY_ID BIGINT NOT NULL,
    USER_ID BIGINT,
    ACTION_TYPE ENUM('ANSWER_SUBMITTED', 'NOTIFICATION_SENT', 'SURVEY_CREATED') NOT NULL,
    METADATA JSON,
    TIMESTAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--     FOREIGN KEY (SURVEY_ID) REFERENCES SURVEYS(ID) ON DELETE CASCADE,
--     FOREIGN KEY (USER_ID) REFERENCES USERS(ID) ON DELETE SET NULL
    );

-- 인사이트 리포트 테이블
CREATE TABLE IF NOT EXISTS INSIGHT_REPORTS (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    SURVEY_ID BIGINT NOT NULL,
    SUMMARY_TEXT TEXT,
    KEYWORDS JSON,
    SENTIMENT_SUMMARY JSON,
    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--     FOREIGN KEY (SURVEY_ID) REFERENCES SURVEYS(ID) ON DELETE CASCADE
);

-- 벡터 임베딩 테이블
CREATE TABLE IF NOT EXISTS QUESTION_EMBEDDINGS (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    QUESTION_ID BIGINT NOT NULL,
    RESPONSE_ID BIGINT NOT NULL,
    EMBEDDING_VECTOR JSON,
    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--     FOREIGN KEY (QUESTION_ID) REFERENCES QUESTIONS(ID) ON DELETE CASCADE,
--     FOREIGN KEY (RESPONSE_ID) REFERENCES RESPONSES(ID) ON DELETE CASCADE
);