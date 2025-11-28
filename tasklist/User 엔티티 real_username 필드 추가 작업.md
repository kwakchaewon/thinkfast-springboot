# User 엔티티 real_username 필드 추가 작업

## 작업 목표
현재 User 엔티티의 `username` 칼럼에는 email이 저장되고 있는데, 별도의 `real_username` 필드를 추가하여 실제 유저 이름을 저장할 수 있도록 수정

## 현재 상황
- **User 엔티티**: `username` 필드에 email이 저장됨 (주석: `// email로 사용`)
- **인증/인가**: `username`을 기반으로 JWT 토큰 생성 및 사용자 인증 처리
- **데이터베이스**: `USER` 테이블에 `USERNAME` 칼럼 존재 (VARCHAR(50), UNIQUE, NOT NULL)

## 작업 범위

### 1. 데이터베이스 스키마 변경
- [ ] Flyway 마이그레이션 스크립트 작성
  - 파일명: `V10__add_real_username_to_user.sql`
  - `USER` 테이블에 `REAL_USERNAME` 칼럼 추가
  - 타입: `VARCHAR(100)` (NULL 허용)
  - 기존 데이터는 NULL로 유지 (기존 사용자는 real_username이 없을 수 있음)

### 2. User 엔티티 수정
- [ ] `src/main/java/com/example/thinkfast/domain/auth/User.java` 수정
  - `realUsername` 필드 추가
  - `@Column(name = "REAL_USERNAME", length = 100)` 어노테이션 추가
  - `@Builder` 패턴 유지 (Lombok이 자동 처리)

### 3. DTO 수정
- [ ] `SignUpRequest` 수정
  - 파일: `src/main/java/com/example/thinkfast/dto/auth/SignUpRequest.java`
  - `realUsername` 필드 추가 (선택적 필드)
  - 기존 `username` 필드는 유지 (email 용도)

- [ ] `TokenResponse` 수정
  - 파일: `src/main/java/com/example/thinkfast/dto/auth/TokenResponse.java`
  - `realUsername` 필드 추가 (선택적)
  - 로그인 응답에 실제 유저 이름 포함

### 4. 서비스 로직 수정
- [ ] `AuthService` 수정
  - 파일: `src/main/java/com/example/thinkfast/service/auth/AuthService.java`
  - `signUp()` 메서드: `SignUpRequest`에서 `realUsername` 받아서 User 엔티티에 저장
  - `login()` 메서드: `TokenResponse`에 `realUsername` 포함하여 반환

### 5. 기존 코드 영향도 분석 및 수정
- [ ] `username` 사용처 확인
  - 인증/인가 관련: `username`은 email로 계속 사용 (변경 없음)
  - 사용자 표시 관련: `realUsername` 사용으로 변경 필요 여부 확인

- [ ] `AlarmMessage` 수정 검토
  - 파일: `src/main/java/com/example/thinkfast/realtime/dto/AlarmMessage.java`
  - 현재 `username` 필드 사용 중
  - 알림 메시지에 표시할 이름을 `realUsername`으로 변경할지 검토

- [ ] `RedisPublisher` 수정 검토
  - 파일: `src/main/java/com/example/thinkfast/realtime/RedisPublisher.java`
  - `userRepository.findUsernameById(userId)` 사용
  - `realUsername`을 반환하는 메서드 추가 필요 여부 확인

- [ ] `UserRepository` 수정
  - 파일: `src/main/java/com/example/thinkfast/repository/auth/UserRepository.java`
  - `findRealUsernameById(Long id)` 메서드 추가 (선택적)

### 6. API 응답 수정
- [ ] 로그인 API 응답에 `realUsername` 포함
  - 엔드포인트: `POST /auth/login`
  - `TokenResponse`에 `realUsername` 필드 추가

- [ ] 회원가입 API 요청에 `realUsername` 필드 추가
  - 엔드포인트: `POST /auth/signup`
  - `SignUpRequest`에 `realUsername` 필드 추가 (선택적)

## 작업 상세 가이드

### 1. 데이터베이스 마이그레이션 스크립트
**파일**: `src/main/resources/db/migration/V10__add_real_username_to_user.sql`

```sql
-- USER 테이블에 REAL_USERNAME 칼럼 추가
ALTER TABLE `USER` 
ADD COLUMN `REAL_USERNAME` VARCHAR(100) NULL AFTER `USERNAME`;

-- 기존 데이터는 NULL로 유지 (마이그레이션 후 사용자가 직접 업데이트)
```

### 2. User 엔티티 수정 예시
```java
@Entity
@Table(name = "USER")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USERNAME", nullable = false, unique = true, length = 100)
    private String username; // email로 사용

    @Column(name = "REAL_USERNAME", length = 100)
    private String realUsername; // 실제 유저 이름

    // ... 기존 필드들
}
```

### 3. SignUpRequest 수정 예시
```java
@Getter
@NoArgsConstructor
public class SignUpRequest {
    private String username; // email
    private String password;
    private String birthDate;
    private String realUsername; // 실제 유저 이름 (선택적)
    
    // ... 기존 메서드들
}
```

### 4. TokenResponse 수정 예시
```java
@Getter
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String username; // email
    private String realUsername; // 실제 유저 이름
    private Role role;
}
```

### 5. AuthService 수정 예시
```java
@Transactional
public void signUp(SignUpRequest request) {
    User user = User.builder()
            .username(request.getUsername())
            .realUsername(request.getRealUsername()) // 추가
            .password(passwordEncoder.encode(request.getPassword()))
            .role(Role.RESPONDER)
            .birthDate(request.getBirthDateAsLocalDate())
            .build();

    userRepository.save(user);
}

@Transactional
public TokenResponse login(LoginRequest request) {
    // ... 기존 로직
    
    User user = userRepository.findByUsername(request.getUsername()).get();

    return TokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .username(user.getUsername())
            .realUsername(user.getRealUsername()) // 추가
            .role(user.getRole())
            .build();
}
```

## 주의사항

### 1. 기존 데이터 호환성
- 기존 사용자의 `realUsername`은 NULL일 수 있음
- API 응답에서 NULL 처리가 필요할 수 있음
- 프론트엔드에서 `realUsername`이 없을 경우 `username`을 표시하도록 처리 필요

### 2. 인증/인가 로직 유지
- `username` 필드는 email로 계속 사용되므로 인증/인가 로직은 변경 없음
- JWT 토큰 생성 시 `username` (email) 사용 유지
- `UserDetailsServiceImpl`, `JwtTokenProvider` 등은 변경 불필요

### 3. 선택적 필드 처리
- `realUsername`은 선택적 필드이므로 NULL 허용
- 회원가입 시 `realUsername`을 제공하지 않아도 가입 가능
- 이후 프로필 수정 기능에서 `realUsername`을 업데이트할 수 있도록 고려

### 4. 기존 코드 영향 최소화
- `username` 필드는 email로 계속 사용되므로 대부분의 코드는 변경 불필요
- 사용자 표시가 필요한 부분만 `realUsername` 사용으로 변경
- 알림, 웹소켓 등에서 사용자 이름 표시 시 `realUsername` 우선 사용

## 테스트 체크리스트

### 단위 테스트
- [ ] User 엔티티에 `realUsername` 필드가 정상적으로 추가되었는지 확인
- [ ] `SignUpRequest`에서 `realUsername`을 받을 수 있는지 확인
- [ ] `TokenResponse`에 `realUsername`이 포함되는지 확인
- [ ] `AuthService.signUp()`에서 `realUsername`이 저장되는지 확인
- [ ] `AuthService.login()`에서 `realUsername`이 응답에 포함되는지 확인

### 통합 테스트
- [ ] 회원가입 API에서 `realUsername`을 포함하여 가입 가능한지 확인
- [ ] 회원가입 API에서 `realUsername` 없이도 가입 가능한지 확인
- [ ] 로그인 API 응답에 `realUsername`이 포함되는지 확인
- [ ] 기존 사용자 로그인 시 `realUsername`이 NULL로 반환되는지 확인

### 데이터베이스 테스트
- [ ] 마이그레이션 스크립트가 정상적으로 실행되는지 확인
- [ ] 기존 데이터에 영향이 없는지 확인
- [ ] 새로운 사용자 생성 시 `realUsername`이 저장되는지 확인

## 다음 작업 (선택적)

### 1. 프로필 수정 API 추가
- 사용자가 `realUsername`을 나중에 수정할 수 있는 API
- 엔드포인트: `PUT /auth/profile` 또는 `PATCH /auth/profile`
- 요청 본문: `{ "realUsername": "새로운 이름" }`

### 2. 사용자 정보 조회 API 수정
- 현재 사용자 정보 조회 API가 있다면 `realUsername` 포함
- 엔드포인트: `GET /auth/me` 또는 `GET /user/profile`

### 3. 알림/웹소켓 메시지 수정
- `AlarmMessage`에서 `realUsername` 사용
- 사용자 이름 표시 시 `realUsername` 우선, 없으면 `username` 사용

## 작업 진행 상황

### 2024-XX-XX
- [ ] 작업 계획 수립 및 task.md 작성
- [ ] 데이터베이스 마이그레이션 스크립트 작성
- [ ] User 엔티티 수정
- [ ] DTO 수정 (SignUpRequest, TokenResponse)
- [ ] AuthService 수정
- [ ] 테스트 및 검증

---

## 참고사항

### 기존 코드 구조
- **인증 방식**: JWT 토큰 기반
- **사용자 식별**: `username` (email) 사용
- **데이터베이스**: MySQL, Flyway 마이그레이션 사용
- **엔티티**: JPA/Hibernate 사용

### 관련 파일 목록
- `src/main/java/com/example/thinkfast/domain/auth/User.java`
- `src/main/java/com/example/thinkfast/dto/auth/SignUpRequest.java`
- `src/main/java/com/example/thinkfast/dto/auth/TokenResponse.java`
- `src/main/java/com/example/thinkfast/service/auth/AuthService.java`
- `src/main/java/com/example/thinkfast/repository/auth/UserRepository.java`
- `src/main/java/com/example/thinkfast/realtime/dto/AlarmMessage.java`
- `src/main/java/com/example/thinkfast/realtime/RedisPublisher.java`
- `src/main/resources/db/migration/V1__create_user_tables.sql`

### 마이그레이션 버전 관리
- 현재 최신 마이그레이션: `V9__add_created_at_to_responses.sql`
- 새 마이그레이션: `V10__add_real_username_to_user.sql`
- 버전 번호는 순차적으로 증가해야 함

