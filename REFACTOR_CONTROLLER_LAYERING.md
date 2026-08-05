# 컨트롤러 계층 위반 정리 (Controller → Service → Repository)

작업일: 2026-08-05

## 배경

일부 컨트롤러가 Service를 거치지 않고 Repository를 직접 호출하고 있어,
`Controller → Service → Repository`로 통일된 흐름에서 벗어나 있었다. 전체
컨트롤러(`src/main/java/com/jipdaum_spring/controller/*.java`, 총 11개)를
조사해 위반 사례를 찾아 Service 계층으로 옮겼다.

## 조사 결과

`controller` 패키지 전체에서 `Repository` 직접 참조를 검색해 다음 2개 파일에서
위반을 발견했다. 나머지 9개(`HomeController`, `AdminProductController`,
`CartController`, `CouponController`, `OrderController`, `ProductController`,
`ChatController`, `AdminCategoryController`, `AdminCouponController`,
`AdminProductOptionController`)는 이미 Service만 의존하고 있어 수정하지 않았다.

### 1. `UserController`

기존에 `UserRepository`(springuser, 소셜 로그인 부가정보 테이블 `users`)와
`CurrentUserProvider`를 컨트롤러가 직접 들고 3개 엔드포인트에서 사용했다.

| 엔드포인트 | 문제 |
|---|---|
| `GET /api/users` (`getAllUsers`) | `userRepository.findAll()`을 컨트롤러에서 직접 스트림 처리 후 `Map`으로 변환 |
| `DELETE /api/users/{id}` (`deleteUser`) | `existsById`/`deleteById`를 컨트롤러에서 직접 호출 |
| `GET /api/users/me` (`getMe`) | `CurrentUserProvider.getCurrentUser()` + `userRepository.findByEmail()`을 컨트롤러에서 직접 조합, `DataAccessException` 캐치도 컨트롤러에 있었음 |

**조치**: 새 `UserService`(`service/UserService.java`)를 만들어 위 3개 메서드
(`getAll`, `delete`, `getMe`)의 로직을 그대로 옮겼다. 응답 형태를 바꾸지
않기 위해 기존에 수기로 만들던 `Map`을 대체하는 DTO를 신규 추가했다:

- `dto/user/UserResponse.java` — `id/email/name/profileImage/provider/role`
  (기존 `getAllUsers`의 Map 키와 동일한 필드명이라 JSON 응답은 그대로다)
- `dto/user/MeResponse.java` — `id/email/name/nickname/profileImage/provider`
  (기존 `getMe`의 Map과 동일)

`deleteUser`의 404 응답(`"User not found"` 문자열 바디)도 그대로 유지하기
위해 `UserService.delete()`는 예외를 던지지 않고 `boolean`(삭제 성공 여부)을
반환하도록 했다 — 컨트롤러의 기존 `if/else` 분기를 그대로 살렸다.

### 2. `AuthController`

`JwtTokenProvider`, `JipdaumUserRepository`, `SocialLoginCodeStore`,
`BlacklistedTokenRepository`를 컨트롤러가 직접 들고 토큰 발급/재발급 로직을
전부 컨트롤러 안에서 처리하고 있었다. 특히 `refresh()`에서
`jipdaumUserRepository.findById(...)`를 직접 호출하는 부분이 계층 위반이었다.

또한 `jwrTokenProvider`라는 오타로 생긴 미사용 중복 필드(`JwtTokenProvider`를
두 번 주입받고 있었음, 어디서도 참조되지 않음)가 있어 죽은 코드로 함께 제거했다.

**조치**: 새 `AuthService`(`service/AuthService.java`)로 `exchangeSocialCode`
(구 `socialExchange`)와 `refresh` 로직을 그대로 옮겼다. 기존 컨트롤러는
에러 상황마다 `ResponseEntity.status(...).body(new ErrorResponse(...))`를
직접 조립했는데, 이를 기존에 있던 `AuthException`을 던지는 방식으로 바꿨다.
`GlobalExceptionHandler`가 `AuthException`을 `{"error": message}` 형태로
직렬화하는 것이 기존 `ErrorResponse(message)`와 정확히 같은 JSON 형태이기
때문에 **응답 바디/상태 코드는 100% 동일**하다 (`ErrorResponse` 레코드의
필드명이 `error`임을 확인하고 진행함).

| 상황 | 기존 응답 | 리팩터 후 |
|---|---|---|
| `code` 누락 | 400, `{"error":"code가 필요합니다."}` | 동일 (AuthException → 동일 JSON) |
| code 만료/무효 | 401, `{"error":"유효하지 않거나 만료된 code입니다."}` | 동일 |
| refresh token 무효/블랙리스트 | 401, `{"error":"Invalid or expired refresh token"}` | 동일 |
| 토큰에서 사용자 식별 불가 | 401, `{"error":"Cannot identify user from token"}` | 동일 |

## 변경/추가 파일

```
신규
  src/main/java/com/jipdaum_spring/service/UserService.java
  src/main/java/com/jipdaum_spring/service/AuthService.java
  src/main/java/com/jipdaum_spring/dto/user/UserResponse.java
  src/main/java/com/jipdaum_spring/dto/user/MeResponse.java

수정
  src/main/java/com/jipdaum_spring/controller/UserController.java
  src/main/java/com/jipdaum_spring/controller/AuthController.java
```

컨트롤러 두 파일 모두 이제 Service 필드만 주입받고, 요청을 그대로
Service에 위임한 뒤 결과를 `ResponseEntity`로 감싸기만 한다.

## 호환성 / 검증

- 엔드포인트 URL, HTTP 메서드, 요청/응답 JSON 스키마, 상태 코드 전부 변경 없음.
- 기존 컨트롤러에 있던 주석(왜 `CurrentUserProvider`를 거쳐야 하는지,
  왜 Django 레거시 토큰을 통과시키는지 등)은 그대로 옮겨서 보존했다.
- `SecurityConfig`의 `/api/users/**` → `ADMIN` 권한 규칙 등 보안 설정은
  변경하지 않았다(엔드포인트 경로가 그대로라 영향 없음).
- `./mvnw.cmd -o compile` 통과 확인.
- MySQL 컨테이너가 떠 있는 환경에서 `./mvnw.cmd test`로 24개 테스트
  전체 통과 여부를 추가로 확인하는 것을 권장한다(이번 세션에서는
  Docker Desktop 미기동으로 통합 테스트는 실행하지 못함).
