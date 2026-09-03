# 집다움(DaumAna) 프로젝트 일정표 요약

## Project Overview
`jipdaum_Springboot`(Spring Boot 백엔드)와 `Team-DaumAna`(React 프론트엔드 + Django 백엔드)
두 저장소로 구성된 듀얼 백엔드 팀 프로젝트다. 2026-06-26 결제/주문 API 초기 커밋을 시작으로
2026-09-03 현재까지 이어지는 실측 기간을 다룬다.

이 일정표는 미래 계획표가 아니라 **두 저장소의 실제 git log(ISO 주차 기준)를 그대로 집계한
회고형 일정표**다 — 각 단계 커밋 수 합계가 실제 총 커밋 수(Spring 90건, DaumAna 195건)와
정확히 일치하도록 검산했다. 원본은 `#Presentation_Document/.../설계문서/08_JIPDAUM_일정표.docx`.

## 진행 중인 이슈 (P0 — 다음 배포 전 확인 필요)

- **JWT 시크릿 git 히스토리 노출**: `application.yml.example`에 실서명키로 쓰일 수 있는 문자열이
  2026-07-21부터 커밋 히스토리에 남아있음. 프로덕션 EC2가 이 값을 그대로 쓰는지 확인 안 됨 —
  확인 전까지 협업자 추가·저장소 공개 전환 보류.
- **EC2 SMTP 환경변수**: GitHub Secrets에 `EMAIL_HOST_USER`/`EMAIL_HOST_PASSWORD`/
  `EMAIL_FROM_ADDRESS` 실값 등록 여부 재확인 필요(미등록 시 빈 문자열 주입되어 발송 실패).
- **Redis 없는 환경 폴백**: EC2에 Redis 컨테이너가 없을 수 있어 캐시 실패 시 DB 직접 조회로
  넘어가는 폴백을 배포 전 실측 확인 완료(`CachingConfigurer.errorHandler()`) — 회귀 여부만 주기 점검.

## 아키텍처 분담 전략

전체 기능을 한 서버에 몰지 않고, **패스워드 인증만 Django에 남기고 나머지(소셜 로그인·상품·
장바구니·주문·결제·쿠폰·AI 챗봇)는 Spring Boot로 이관**하는 구조로 정착했다(`HANDOFF_AUTH_SCOPE.md`).
두 서버는 같은 MySQL 인스턴스와 JWT secret을 공유해 서로의 토큰을 상호 인식한다.

AI 챗봇도 단일 대형 프롬프트 대신 RAG(임베딩 의미 검색) + 함수 호출(tool-calling)로 상품 조회를
분리했고, 상품 목록 조회는 Redis 30초 TTL 캐싱으로 응답 시간을 534ms → 28ms(약 19배)로 줄였다.

## Execution Timeline (실측)

| 단계 | 기간 | 구분 | 주요 내용 | Spring 커밋 | DaumAna 커밋 |
|---|---|---|---|---|---|
| 1 | 6/22~7/5 (W26-27) | 백엔드 기반 구축 | jipdaum_Springboot 최초 커밋. 결제 시스템 통합, 주문/쿠폰/상품 API 1차 구현 | 5 | - |
| 2 | 7/13~7/26 (W29-30) | 프론트·백엔드 동시 착수, 인증 이관 | Team-DaumAna 생성, 초기 프론트 구현체 커밋. Spring은 챗봇/카테고리 API 포팅, 소셜 로그인 안정화, Django 로컬 계정 인증 포팅, MySQL 드라이버 전환 | 12 | 20 |
| 3 | 7/27~8/9 (W31-32) | 결제·보안 보완, 관리자 기능 | OWASP 점검 후 인증/권한 보완, 재로그인/탈퇴 복구 버그 수정. 관리자 CRUD 추가, 컨트롤러 계층 정리, Docker/CI 워크플로 추가 | 12 | 17 |
| 4 | 8/10~8/16 (W33) | LLM 챗봇 도입 | Gemini 기반 챗봇 도입(stateless 멀티턴 + tool-calling), 챗봇 UI 리디자인 | 1 | 3 |
| 5 | 8/17~8/23 (W34) | 통합 마무리 스프린트 | Django를 admin 전용으로 축소, 전 API Spring 이관 완료. RAG·rate limit·hCaptcha 게이트 추가. EC2 자동 배포 CI 추가 | 32 | 46 |
| 6 | 8/24~8/30 (W35) | 데이터 정비·DB 이관·포트폴리오 착수 | Oracle → MySQL 이관 완료. `GlobalExceptionHandler`에 예외 핸들러 추가(302 리다이렉트 버그 원인 제거). 프로덕션 SMTP 발송 실패 진단·수정 | 14 | 55 |
| 7 | 8/31~9/3 (W36) | 회귀 방지·성능 개선·마무리 | 장애 3건 케이스 스터디화 + 회귀 테스트 9건 추가. 상품 목록 Redis 캐싱 + 동시성 부하 테스트 추가. 카카오 QR 로그인 회귀 대응 | 14 | 54 |

## 배포·인프라 현황

- Docker Hub 이미지 빌드/푸시 → EC2 `docker run`으로 자동 배포(`main` 브랜치 push 트리거).
- MySQL(`jibdaum`, 3306)은 Django 마이그레이션이 스키마를 소유 — Spring `ddl-auto: none` 고정,
  `Team-DaumAna/docker-compose.yml`이 컨테이너(`jibdaum-mysql`, `jibdaum-redis`) 관리.
- `./mvnw.cmd test` 기준 34개 테스트 전부 통과(MySQL 컨테이너 기동 시).
