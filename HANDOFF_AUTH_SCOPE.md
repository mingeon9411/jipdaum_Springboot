# 인수인계: Django ↔ Spring Boot 기능 분담 확정

Django(`c:\Pro_Jipdaum\Team-DaumAna\backend`) 쪽 Claude Code 세션에서 조사한 내용을 바탕으로
팀에서 아래와 같이 역할 분담을 확정했습니다. Spring Boot 프로젝트에서 이어서 작업할 때 참고해주세요.

## 결정 사항

- **Django에 남기는 것**: 패스워드 기반 인증만 (`backend/Users` 앱)
  - 회원가입 (`RegisterView`)
  - 로그인 (`LoginView` — hCaptcha 검증 + JWT 발급)
  - 로그아웃 (`LogoutView` — refresh 토큰 블랙리스트)
  - 닉네임 중복확인 (`NicknameCheckView`)
  - 이메일 OTP 발송/검증 (`EmailOTPSendView`/`EmailOTPVerifyView`)
- **Spring Boot로 옮기는(또는 이미 옮겨져 있는) 것**: 나머지 전부
  - 소셜 로그인 (Kakao/Naver/Google) — 이미 구현됨 (`security/oauth/*`)
  - `/me`, JWT refresh — 이미 구현됨 (`UserController`, `AuthController`)
  - 상품/장바구니/리뷰 — `service/ProductService.java`, `CartService.java` 등 이미 존재
  - 주문/결제(PortOne) — `service/OrderService.java`, `domain/order/Payment.java` 등 이미 존재
  - 쿠폰 — `service/CouponService.java` 등 이미 존재

## 조사 배경

- 프론트엔드(`frontend/src/api.js`)는 이미 로그인/회원가입을 제외한 거의 모든 API(장바구니, 상품, 리뷰,
  주문, 쿠폰, 챗봇, `/me`, 소셜로그인)를 Spring Boot(port 8081)로 호출하도록 되어 있음.
- Django의 `Users/users.py`에 `SpringBootCompatToken`이 이미 존재 — Spring이 발급한 JWT
  (`sub`=email 또는 `kakao_xxx`, `token_type`/`jti` 없음)를 Django가 그대로 검증하도록 맞춰둔 상태.
  Spring의 `JwtTokenProvider`도 반대로 Django SimpleJWT 토큰(`user_id` 클레임)을 인식하는 호환 코드가
  있음 — 양쪽이 서로의 토큰을 읽을 수 있게 이미 설계돼 있음.
- 다만 Spring Boot 쪽에는 **패스워드 기반 회원가입/로그인/로그아웃/닉네임체크/이메일OTP가 전혀 없음**
  (`User` 엔티티에 비밀번호 필드 자체가 없고, mail/captcha 의존성도 pom.xml에 없음). 그래서 이 부분만
  당분간 Django에 남겨두기로 함.

## Spring Boot 쪽에 요청하는 작업

1. `service/ProductService`, `CartService`, `OrderService`, `CouponService` 등이 Django의
   `backend/Products`, `backend/Orders`, `backend/coupons`, `backend/payments` 앱과 **기능 동등성**이
   있는지 감사(audit) 부탁드립니다. 빠진 기능이 있으면 포팅해주세요.
2. 확인이 끝나면 Django 쪽의 `Products`/`Orders`/`coupons`/`payments` 앱과 `Users` 앱의
   비-패스워드 기능(소셜 로그인 관련 죽은 코드 `adapters.py` 등)은 삭제할 예정입니다.
3. 참고로 현재 `UserController`의 `/me`는 Spring 자체 `User` 엔티티의 `name`을 `nickname`으로 반환하고
   있어, Django와 공유하는 `JIPDAUM_USER.nickname`과 실제로 동기화되는지는 별도 확인이 필요해 보입니다.

## Django 쪽 다음 작업 (참고용, Spring 쪽에서 할 일은 아님)

- `backend/Products`, `backend/Orders`, `backend/coupons`, `backend/payments` 앱 삭제
- `backend/Users/adapters.py` (allauth 죽은 코드), `backend/Users/signals.py`의 세션 로그인 시그널 삭제
- `Users` 앱은 회원가입/로그인/로그아웃/닉네임체크/이메일OTP만 남기고 정리
