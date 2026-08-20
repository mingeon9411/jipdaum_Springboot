<div align="center">

<h1>jipdaum_Springboot</h1>

<p>
<b>집다움</b> — 한국 전통 감성을 담은 가구 · 인테리어 D2C 플랫폼<br/>
<code>Team-DaumAna</code>(React · Django) 팀 프로젝트의 <b>Spring Boot 백엔드</b>입니다.
</p>

<p>
<code>Team-DaumAna</code>와 <b>같은 MySQL 인스턴스</b>를 공유하는 듀얼 백엔드 구조에서,<br/>
패스워드 인증을 제외한 <b>상품 · 장바구니 · 주문 · 결제 · 쿠폰 · 소셜 로그인 · AI 챗봇</b>을 전담합니다.
</p>

</div>

<br/>

---

## 🛠 Tech Stack

**Backend**

[![](https://skillicons.dev/icons?i=java,spring)](https://skillicons.dev)
&nbsp;
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)

**Database**

[![](https://skillicons.dev/icons?i=mysql)](https://skillicons.dev)
<br/>
<sub>MySQL — Django(회원 · 상품 마스터) · Spring Boot(장바구니 · 주문 · 결제) 공용, 스키마는 Django 마이그레이션 소유(`ddl-auto: none`)</sub>

**Auth**

![Kakao](https://img.shields.io/badge/Kakao-FFCD00?style=flat-square&logo=kakao&logoColor=black)
![Naver](https://img.shields.io/badge/Naver-03C75A?style=flat-square&logo=naver&logoColor=white)
![Google](https://img.shields.io/badge/Google-4285F4?style=flat-square&logo=google&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)
&nbsp;<sub>Spring OAuth2 Client + JJWT, Django 발급 JWT와 secret 공유로 양쪽 서버 상호 인식</sub>

**AI 챗봇**

![Gemini](https://img.shields.io/badge/Gemini_API-8E75B2?style=flat-square&logo=googlegemini&logoColor=white)
&nbsp;<sub>gemini-3.6-flash 대화 + gemini-embedding-001 기반 RAG 의미 검색, 함수 호출(search_products/get_product_detail)</sub>

**Payment**

![PortOne V2](https://img.shields.io/badge/PortOne_V2-6C1EF2?style=flat-square&logoColor=white)

**Rate Limiting / Cache**

![Bucket4j](https://img.shields.io/badge/Bucket4j-2C3E50?style=flat-square&logoColor=white)
![Caffeine](https://img.shields.io/badge/Caffeine-6F4E37?style=flat-square&logoColor=white)

**DevOps**

[![](https://skillicons.dev/icons?i=docker,git,github,githubactions)](https://skillicons.dev)

<br/>

---

## 🏠 담당 기능

Django ↔ Spring 기능 분담 배경은 [`HANDOFF_AUTH_SCOPE.md`](HANDOFF_AUTH_SCOPE.md) 참고.
요지: **패스워드 인증**(회원가입 · 로그인 · 로그아웃 · 닉네임체크 · 이메일OTP)만 Django에 남고,
**나머지 전부**는 Spring Boot 담당입니다.

**Spring Boot API** &nbsp;`Port 8081`

| | |
|---|---|
| 🔐 **인증** | 소셜 로그인(카카오 · 네이버 · 구글) 토큰 교환, hCaptcha 검증, JWT 리프레시 — Django 발급 JWT와 상호 호환 |
| 🛍 **상품** | 검색(진열/collection 스코프 — 메인 · 한국관) · 카테고리 필터 · 상품 상세 · 리뷰 조회 · 작성 |
| 🛒 **장바구니** | 조회 · 추가 · 수정 · 삭제 |
| 📦 **주문 · 결제** | 주문 생성 · PortOne V2 결제 준비/검증 · 결제 완료 · 취소 · 주문 내역 |
| 🎟 **쿠폰** | 내 쿠폰 조회 · 쿠폰 검증 |
| 🤖 **AI 챗봇** | Gemini 기반 상품 상담 — RAG 임베딩 의미 검색 + 함수 호출로 상품 추천/상세 안내, Bucket4j로 IP/계정당 요청 제한 |
| 🛠 **관리자** | 카테고리 · 상품 · 상품 옵션 · 쿠폰 CRUD |
| 📁 **파일 업로드** | 상품 이미지 등 파일 업로드 |
| 🔗 **인증 연동** | `jwt.secret`을 Django `SECRET_KEY`와 공유 — 프론트가 단일 토큰으로 양쪽 서버 호출 |

<br/>

---

## ⚙️ 로컬 실행

1. `Team-DaumAna/docker-compose.yml`로 MySQL(`jibdaum-mysql`) 컨테이너 먼저 기동
   — 스키마는 Django 마이그레이션이 소유하므로 컨테이너 관리도 그 저장소 쪽에서 합니다.
2. `src/main/resources/application.yml.example`을 복사해 `application.yml` 생성
   (시크릿 포함 파일이라 git에 없음 — 로컬 컨테이너 자격증명 · 더미 mail/oauth/portone 값이 채워져 있어 바로 동작)
3. `jwt.secret`은 Django `backend/.env`의 `SECRET_KEY`와 **반드시 같은 값**이어야 토큰이 양쪽에서 호환
   (example 파일에 이미 그 값이 들어있음)
4. `./mvnw.cmd spring-boot:run`

`./mvnw.cmd test` — MySQL 컨테이너가 떠 있으면 24개 테스트 전부 통과 (`contextLoads` 포함)

## 알려진 상태

- `spring.mail`은 더미 SMTP 값이라 실제 이메일 발송은 안 됨 (부팅은 정상)
- Django와 스키마를 공유하므로 `application.yml`의 `ddl-auto`는 반드시 `none`
