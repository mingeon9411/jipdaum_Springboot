# jipdaum_Springboot

## DB: MySQL, Django와 공유

- 예전엔 Oracle을 썼지만 지금은 MySQL(`jibdaum` DB, 3306)로 전환됨.
- 이 MySQL은 `C:/Pro_Jipdaum/Team-DaumAna` 저장소(Django 백엔드)와 **같은 인스턴스를 공유**한다.
  스키마(테이블)는 Django 마이그레이션이 소유하므로, `application.yml`의 `ddl-auto`는 반드시 `none`.
- 컨테이너는 Spring 쪽이 아니라 `Team-DaumAna/docker-compose.yml`이 관리한다 (`jibdaum-mysql`).
  로컬에서 Spring을 띄우기 전에 그 저장소에서 `docker compose up -d`로 먼저 켜야 함.
- 자세한 Django/Spring 기능 분담은 `HANDOFF_AUTH_SCOPE.md` 참고. 요지: 패스워드 인증(회원가입/로그인/
  로그아웃/닉네임체크/이메일OTP)은 Django에 남고, 나머지(소셜로그인/상품/장바구니/주문/쿠폰 등)는 Spring.

## application.yml은 git에 없음

- `src/main/resources/application.yml`은 시크릿(DB 비번, JWT secret 등) 때문에 `.gitignore` 대상이라
  새로 클론/풀 받은 PC에는 이 파일 자체가 없다 — 없으면 빌드는 되지만 앱 구동/테스트가 안 됨.
- `src/main/resources/application.yml.example`을 복사해서 `application.yml`로 만들면 바로 동작하는
  값(로컬 MySQL 컨테이너 자격증명, 더미 mail/oauth/portone 값)이 채워져 있다.
- `jwt.secret`은 Django의 `SECRET_KEY`(`backend/.env`)와 반드시 같은 값이어야 토큰이 양쪽에서 호환됨
  (example 파일에 이미 그 값이 들어있음).

## 알려진 상태

- `spring.mail`은 더미 SMTP 값이라 실제 이메일 발송은 안 됨(부팅은 정상). 실발송 테스트하려면
  실제 Gmail 앱 비밀번호로 교체 필요.
- `./mvnw.cmd test` 기준 24개 테스트 전부 통과 (MySQL 컨테이너가 떠 있어야 `contextLoads` 통과).
