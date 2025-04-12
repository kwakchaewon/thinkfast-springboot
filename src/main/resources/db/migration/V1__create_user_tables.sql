CREATE TABLE IF NOT EXISTS `USER` (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    USERNAME VARCHAR(50) NOT NULL UNIQUE,
    PASSWORD VARCHAR(255) NOT NULL,
    EMAIL VARCHAR(100) NOT NULL UNIQUE,
    ROLE ENUM('ADMIN', 'CREATOR', 'RESPONDER') NOT NULL DEFAULT 'RESPONDER',
    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS REFRESH_TOKENS (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    USER_ID BIGINT NOT NULL,
    TOKEN VARCHAR(255) NOT NULL UNIQUE,
    EXPIRES_AT TIMESTAMP NOT NULL,
    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--     FOREIGN KEY (USER_ID) REFERENCES USERS(ID) ON DELETE CASCADE
);