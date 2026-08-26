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

- `spring.mail`은 로컬에서는 실발송 가능한 상태다: `application.yml`이
  `smtp.naver.com` + `${EMAIL_HOST_USER:}`/`${EMAIL_HOST_PASSWORD:}`를 쓰고, 이 값들은
  Windows 사용자 환경변수(`EMAIL_HOST_USER`/`EMAIL_HOST_PASSWORD`/`EMAIL_FROM_ADDRESS`)로
  실제 네이버 계정 값이 등록돼 있음(`application.yml.example`의 dummy Gmail 값과는 다름).
  `UserAuthService.sendEmailOtp`가 `JavaMailSender`로 이걸 사용한다.
- **프로덕션(EC2)은 원래 이 env var들을 못 받는 구조였다** — `.github/workflows/docker-publish.yml`의
  배포 스텝이 `docker run`에 env var 주입 없이 `/opt/jipdaum/config/application.yml`을 호스트에서
  그대로 마운트만 했음. 2026-08-26에 실사용자 가입 플로우에서 SMTP 발송 실패가 실제로 확인돼
  `docker run`에 `-e EMAIL_HOST_USER/-e EMAIL_HOST_PASSWORD/-e EMAIL_FROM_ADDRESS`
  (GitHub Secrets 참조)를 추가했다. **GitHub 저장소 Settings → Secrets에 같은 이름의 Secret 3개를
  실제 값으로 등록해야** 다음 배포부터 반영됨(등록 전엔 빈 문자열이 주입되어 여전히 실패).
- `HANDOFF_AUTH_SCOPE.md`(2026-07-14 작성)는 "이메일OTP는 Django에 남긴다"고 적혀 있지만
  이후(`a946b2c`) 실제로 Spring `UserAuthService`로 포팅됐다 — 이 문서는 갱신 안 된 옛 스냅샷이니
  기능 분담 최신 상태는 코드(`UserController`/`UserAuthService`) 기준으로 판단할 것.
- `./mvnw.cmd test` 기준 24개 테스트 전부 통과 (MySQL 컨테이너가 떠 있어야 `contextLoads` 통과).
