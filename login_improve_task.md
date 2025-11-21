# 로그인 로직 개선 작업 리스트

## 🔴 Critical (즉시 수정 필요)

### 1. UserDetailImpl.getAuthorities() null 반환 문제 수정 ✅
- **파일**: `src/main/java/com/example/thinkfast/security/UserDetailImpl.java`
- **문제**: `getAuthorities()` 메서드가 null을 반환하여 Spring Security의 권한 기반 접근 제어가 작동하지 않음
- **수정 내용**: 
  ```java
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
      return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + this.roles.name()));
  }
  ```
- **영향**: 권한 기반 접근 제어가 전혀 작동하지 않음
- **상태**: ✅ 개선 완료

### 2. UserDetailImpl.responderBuild() 하드코딩된 역할 수정 ✅
- **파일**: `src/main/java/com/example/thinkfast/security/UserDetailImpl.java`
- **문제**: 모든 사용자를 `Role.RESPONDER`로 하드코딩하여 실제 사용자 역할이 반영되지 않음
- **수정 내용**:
  ```java
  public static UserDetailImpl responderBuild(User user){
      return new UserDetailImpl(user.getUsername(), user.getPassword(), user.getRole());
  }
  ```
- **영향**: 역할 기반 접근 제어가 올바르게 작동하지 않음
- **상태**: ✅ 개선 완료

### 3. RefreshToken 만료 검증 추가 ✅
- **파일**: `src/main/java/com/example/thinkfast/service/auth/AuthService.java`
- **문제**: `refreshToken()` 메서드에서 DB의 `expiryDate`를 확인하지 않음
- **수정 내용**: 
  ```java
  if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
      refreshTokenRepository.delete(storedToken);
      throw new RuntimeException("만료된 리프레시 토큰입니다.");
  }
  ```
- **위치**: `refreshToken()` 메서드 내부, `storedToken` 검증 후
- **영향**: 만료된 토큰으로도 새 토큰을 발급받을 수 있는 보안 취약점
- **상태**: ✅ 개선 완료
- **프론트엔드 가이드**: `FRONTEND_TOKEN_REFRESH_GUIDE.md` 참고

---

## 🟠 High (높은 우선순위)

### 4. 인증 예외 처리 추가
- **파일**: `src/main/java/com/example/thinkfast/common/aop/GlobalExceptionHandler.java`
- **문제**: `BadCredentialsException`, `UsernameNotFoundException` 등 인증 관련 예외가 처리되지 않음
- **수정 내용**: 
  ```java
  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<BaseResponse> handleBadCredentialsException(BadCredentialsException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(BaseResponse.fail(ResponseMessage.INVALID_CREDENTIALS));
  }
  
  @ExceptionHandler(UsernameNotFoundException.class)
  public ResponseEntity<BaseResponse> handleUsernameNotFoundException(UsernameNotFoundException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(BaseResponse.fail(ResponseMessage.USER_NOT_FOUND));
  }
  ```
- **추가 작업**: `ResponseMessage`에 `INVALID_CREDENTIALS`, `USER_NOT_FOUND` 추가 필요

### 5. 로그아웃 시 토큰 검증 추가
- **파일**: `src/main/java/com/example/thinkfast/service/auth/AuthService.java`
- **문제**: `logout()` 메서드에서 토큰 유효성 검증 없이 사용자명 추출
- **수정 내용**:
  ```java
  @Transactional
  public void logout(String token) {
      String bearerToken = jwtTokenProvider.extractBearerToken(token);
      if (!jwtTokenProvider.validateToken(bearerToken)) {
          throw new RuntimeException("유효하지 않은 토큰입니다.");
      }
      String username = jwtTokenProvider.getUsername(bearerToken);
      refreshTokenRepository.findByUsername(username)
              .ifPresent(refreshTokenRepository::delete);
  }
  ```
- **영향**: 유효하지 않은 토큰으로도 로그아웃이 가능한 보안 취약점

### 6. refreshToken 메서드 중복 코드 정리
- **파일**: `src/main/java/com/example/thinkfast/service/auth/AuthService.java`
- **문제**: 108-128 라인에 중복된 RefreshToken 저장 로직
- **수정 내용**: 
  - 기존 리프레시 토큰 삭제 후 새 토큰 저장하는 단순한 로직으로 변경
  - 불필요한 중복 체크 제거
- **영향**: 코드 가독성 및 유지보수성 저하

---

## 🟡 Medium (중간 우선순위)

### 7. SecurityConfig의 deprecated 메서드 교체
- **파일**: `src/main/java/com/example/thinkfast/security/SecurityConfig.java`
- **문제**: Spring Security 6.x에서 `antMatchers()`가 deprecated됨
- **수정 내용**: 
  ```java
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/public/**").permitAll()
      .requestMatchers("/auth/**").permitAll()
      .requestMatchers("/admin/**").hasRole("ADMIN")
      .requestMatchers("/creator/**").hasRole("CREATOR")
      .anyRequest().authenticated()
  )
  ```
- **영향**: 향후 Spring Security 버전 업그레이드 시 호환성 문제

### 8. 커스텀 예외 클래스 도입
- **파일**: 새로 생성 필요
- **문제**: `RuntimeException`을 직접 던져 구체적인 예외 처리가 어려움
- **수정 내용**: 
  - `InvalidTokenException`, `TokenExpiredException`, `RefreshTokenNotFoundException` 등 커스텀 예외 클래스 생성
  - `AuthService`에서 `RuntimeException` 대신 커스텀 예외 사용
- **영향**: 예외 처리의 일관성 및 명확성 향상

### 9. JWT Secret Key 처리 개선
- **파일**: `src/main/java/com/example/thinkfast/security/JwtTokenProvider.java`
- **문제**: 문자열을 직접 바이트로 변환하면 인코딩 문제 발생 가능
- **수정 내용**:
  ```java
  @PostConstruct
  protected void init() {
      byte[] keyBytes = Decoders.BASE64.decode(secretKey);
      this.key = Keys.hmacShaKeyFor(keyBytes);
  }
  ```
- **추가 작업**: `application.properties`에서 secret key를 BASE64로 인코딩하여 저장
- **영향**: 인코딩 문제로 인한 보안 취약점 가능성

---

## 🟢 Low (낮은 우선순위)

### 10. 로그인 실패 횟수 제한 기능 추가
- **파일**: 
  - `src/main/java/com/example/thinkfast/domain/auth/User.java` (실패 횟수 필드 추가)
  - `src/main/java/com/example/thinkfast/service/auth/AuthService.java` (실패 횟수 체크 로직)
- **문제**: 무차별 대입 공격(Brute Force Attack)에 취약
- **수정 내용**:
  - User 엔티티에 `failedLoginAttempts`, `accountLocked`, `lockedUntil` 필드 추가
  - 로그인 실패 시 실패 횟수 증가
  - 일정 횟수 초과 시 계정 잠금
  - 일정 시간 후 자동 잠금 해제
- **영향**: 보안 강화 (선택적 기능)

---

## 작업 체크리스트

- [x] 1. UserDetailImpl.getAuthorities() null 반환 문제 수정 ✅
- [x] 2. UserDetailImpl.responderBuild() 하드코딩된 역할 수정 ✅
- [x] 3. RefreshToken 만료 검증 추가 ✅
- [ ] 4. 인증 예외 처리 추가
- [ ] 5. 로그아웃 시 토큰 검증 추가
- [ ] 6. refreshToken 메서드 중복 코드 정리
- [ ] 7. SecurityConfig의 deprecated 메서드 교체
- [ ] 8. 커스텀 예외 클래스 도입
- [ ] 9. JWT Secret Key 처리 개선
- [ ] 10. 로그인 실패 횟수 제한 기능 추가

---

## 참고사항

- 각 작업은 독립적으로 수행 가능하지만, Critical 항목은 우선적으로 처리 필요
- 작업 완료 후 테스트 필수:
  - 로그인/로그아웃 기능 테스트
  - 토큰 갱신 기능 테스트
  - 권한 기반 접근 제어 테스트
  - 예외 처리 테스트
- 보안 관련 변경사항은 코드 리뷰 필수

