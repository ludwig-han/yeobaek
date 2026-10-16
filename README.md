# 여백

중요한 목표와 제약을 남기고, 필요한 웹 조사를 Gemini에 요청하는 하루 계획 도구.
계획 작성·수정·공유는 조사 기능을 켜지 않아도 사용할 수 있다.

Java 17 · Spring Boot 3.5.16 · Maven · Thymeleaf · Spring Data JPA · Spring Security · Flyway.

## 로컬 실행

JDK 17과 Maven이 필요하다. 저장소 루트에서 실행한다. Maven Wrapper는 포함되어 있지 않다.

```sh
mvn verify
mvn spring-boot:run
```

기본 주소는 `http://localhost:8080`. 별도 DB 설치나 필수 환경변수 없이 실행되며,
H2 파일 DB가 `./data/yeobaek`에 생성된다. `data/`는 Git에 넣지 않는다.
DB 파일을 삭제하면 로컬 계획도 사라진다.

Windows에서 IntelliJ의 Bundled Maven을 사용하는 경우 Maven 도구 창에서 `verify` 후 실행할 수 있다.

```powershell
.\scripts\run-local.ps1
# 다른 포트: .\scripts\run-local.ps1 -Port 8081
```

스크립트는 빌드된 `target/app.jar`를 실행한다. Java는 PATH, JAVA_HOME, IntelliJ의 JBR 순으로 찾는다.

## 설정

| 환경변수 | 기본값 | 용도 |
|---|---|---|
| `PORT` | `8080` | HTTP 포트. 실행 스크립트에서는 `-Port` 인수가 우선한다. |
| `RESEARCH_ENABLED` | `false` | Gemini 조사 활성화 |
| `GEMINI_API_KEY` | 없음 | 조사용 서버 API 키 |
| `GEMINI_MODEL` | 없음 | 사용할 모델 ID. 현재 코드는 `gemini-3`으로 시작하는 ID를 허용한다. |
| `RESEARCH_DAILY_LIMIT` | `10` | 서울 날짜 기준 앱 전체의 일일 조사 요청 한도 |

조사는 활성화 설정과 키·모델이 모두 필요하다. 실제 모델 사용 가능 여부와 검색 권한·할당량은
해당 Gemini 프로젝트에서 확인한다. 요청 횟수 제한은 금액 상한이 아니다.
키는 소스나 Git에 넣지 않는다. Spring은 `.env`를 자동으로 읽지 않는다.
Windows 실행 스크립트는 현재 프로세스에 키가 없으면 사용자 환경변수의 `GEMINI_API_KEY`를 읽는다.

## 테스트

```sh
mvn test
mvn verify
```

테스트는 메모리 H2와 로컬 HTTP 서버/테스트 대역을 사용한다. 실제 Gemini 호출은 하지 않는다.
GitHub Actions와 Docker 빌드에서도 `mvn -B verify`를 실행한다.
현재 자동 검증에 실제 PostgreSQL이나 브라우저 JavaScript 실행은 포함되어 있지 않다.

## 배포 설정

`Dockerfile`과 `render.yaml`에 Render 배포 구성이 있다. `prod` 프로필은 PostgreSQL을 사용한다.

| 환경변수 | prod 설정 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | 필수 |
| `DB_PORT` | 기본 `5432` |

Blueprint는 DB 연결 값을 주입하고 조사를 활성화한다. 키와 모델은 별도로 설정한다.
prod 세션 쿠키에는 HTTPS가 필요하다. `/healthz`는 HTTP 생존 확인이며 DB·Gemini 상태는 검사하지 않는다.
실제 배포 브랜치와 환경변수, 운영 반영 여부는 저장소 설정만으로 확정할 수 없다.

스키마는 Flyway로 관리하며 Hibernate는 검증만 한다. 적용된 migration을 고치는 대신 새 버전을 추가한다.

## 작업 위치

- `src/main/java`: 서버 코드
- `src/main/resources`: 설정, 화면 템플릿, 정적 파일, DB migration
- `src/test/java`: 자동 테스트
- `scripts`: 로컬 실행 보조 스크립트
- `docs`: [구성 개요](docs/architecture.md), [기존 제품 기록](docs/PRODUCT.md), [ADR](docs/adr/)

현재 작업과 첫 배정은 [작업 백로그](docs/training-backlog/README.md)에서 확인한다.
조회 링크와 수정 키는 별개다. 수정 키는 별도로 보관해야 하며, ‘내 계획’ 목록은 같은 브라우저에서만 유지된다.
