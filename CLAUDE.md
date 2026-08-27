# jipdaum_Springboot

## 작업 워크플로우 (필수 준수)

- **검수 = 실제 디버깅.** 코딩 작업을 검수할 때는 눈으로 diff만 보지 말고 반드시 실제로
  빌드/테스트/실행해서 디버깅한다.
- **버그 발견 시 먼저 보고, 허락받고 수정.** 디버깅 중 코드상의 오류나 버그가 발견되면
  임의로 바로 고치지 말고, 먼저 사용자에게 알리고 수정 허락을 받은 뒤에 고친다.
- **완료 시 자동 커밋+배포.** (위 디버깅/승인 절차가 끝난) 코딩 작업이 완료되면 별도로
  묻지 않고 자동으로 깃허브에 커밋 후 배포까지 진행한다.
- **민감정보는 절대 커밋 금지.** DB 비밀번호, JWT secret, SMTP 자격증명, API 키 등
  민감한 정보가 변경에 포함되면 반드시 사용자에게 먼저 알리고, 깃허브에는 커밋하지 않는다
  (환경변수/GitHub Secrets/`.gitignore` 대상 파일로 분리).

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

## 백엔드 변경 시 자체 검수 체크리스트

컨트롤러/DTO/보안 설정을 건드리는 작업을 끝내기 전에 아래를 확인한다. 무거운 전수조사가 아니라
변경이 닿은 범위에서만 확인하면 됨.

- **예외가 프론트까지 JSON으로 도달하는가**: 새 예외 타입을 던지는 코드를 추가했다면
  `GlobalExceptionHandler`에 걸리는지 확인. 여기 안 걸리면 Boot 기본 `/error` 포워드가
  `SecurityConfig`의 `anyRequest().authenticated()`에 걸려 `/login`으로 302 리다이렉트되고,
  axios는 그 리다이렉트를 그대로 따라가다 크래시난다(각 핸들러 주석 참고).
  (2026-08-27: `@Valid` 검증 실패(`MethodArgumentNotValidException`)와 그 외 처리 안 된 모든
  예외(`Exception`)가 이 핸들러에 안 걸리고 있던 걸 발견해 추가함 — 특히 회원가입/로그인/장바구니/
  주문/결제 등 `@Valid`를 쓰는 19개 엔드포인트가 실제로 이 경로를 탈 수 있었음.)
- **에러 응답 모양이 프론트 기대와 맞는가**: 필드별 폼 에러는 `{"<field>": message}`
  (`FieldValidationException` 주석 — `Register.jsx`가 `err.response.data.<field>`로 읽음),
  그 외 일반 오류는 `ErrorResponse`(`{"error": message}`). 새 핸들러를 추가할 때 이 두 관례 중
  프론트가 실제로 기대하는 쪽에 맞출 것 — 임의로 새 모양을 만들지 말 것.
- **`SecurityConfig.authorizeHttpRequests` 순서**: 매처는 먼저 매칭되는 규칙이 이긴다. 새
  엔드포인트를 추가했으면 의도한 규칙(permitAll/authenticated/hasRole)이 그 앞의 더 넓은
  패턴에 먼저 걸려 무시되지 않는지 확인.
- **데드 코드**: 새로 추가한 `@Component`/서비스 메서드가 실제로 호출/주입되는지 확인
  (Spring 빈은 컴파일러가 안 잡아주므로 grep으로 직접 확인 필요). 안 쓰는 컨트롤러 엔드포인트나
  주석 처리된 코드는 남기지 말고 지울 것.
- **보안 스팟체크**: 새 엔드포인트가 클라이언트가 보낸 id(userId 등)를 그대로 신뢰하지 않고
  `CurrentUserProvider`(SecurityContext)로 소유권을 검증하는지, 새 SQL이 `nativeQuery` 문자열
  결합이 아닌지, 새 파일 업로드가 확장자 allowlist + 크기 제한 + 랜덤 파일명을 쓰는지
  (`FileUploadController` 패턴 참고) 확인.
