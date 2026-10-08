# 여백 v0.1

사용자가 중요한 것을 결정하고, 앱은 그 구조를 보존하며, Gemini가 웹 조사를 맡는 하루 계획 도구.
Java 17 · Spring Boot 3.5.16 · Thymeleaf · JPA · Flyway.

2026-10-08 로컬 검증: Maven 3.9.11 / Java 21.0.9에서 `verify` 성공, 24개 테스트 통과(컴파일 대상 Java 17).
실제 파일 H2 마이그레이션·앱 실행·브라우저 생성/수정/공유를 확인했습니다.
내장 브라우저의 실제 폭은 705px로 유지되어 390px 모바일 실측 검증은 완료하지 못했습니다. 모바일 CSS는 유지합니다.
Render 배포 완료(사용자 확인), 공개 주소: https://yeobaek.onrender.com/ . 운영 환경의 생성·조사·공유·재시작 후 데이터 유지 검증은 별도로 기록합니다.
Gemini Interactions 기반 브라우저 조사 1회(검색 + 도구 없는 JSON 추출) 성공: 출처 9개, 검색어 3개, 충돌·미확인 표시, 새로고침 후 저장 결과 유지, 웨이팅 60→45분 수정 후 재조사 필요 표시를 확인했습니다.

## 로컬 실행

```sh
mvn verify
mvn spring-boot:run
```

Windows에서 Maven/Java가 PATH에 없으면 IntelliJ의 Bundled Maven으로 `verify` 후:

```powershell
.\scripts\run-local.ps1
```

스크립트는 Java 또는 IntelliJ의 JBR을 사용하고 사용자 환경변수의 Gemini 키를 읽습니다. 키는 출력하지 않습니다.
기본 주소는 http://localhost:8080, DB는 `./data/yeobaek` 파일 H2입니다.
`data/`는 Git에 포함하지 마세요. DB를 지우면 기존 계획을 잃습니다.

## 입력과 권한

- Basic: 제목·날짜·지역. 필수는 제목·날짜·첫 Anchor.
- Anchor: 최대 2개. 유형, 후보 장소, EXACT/REPLACEABLE/미정, 시간 제약, 최소 체류, Backup 필요, 짧은 메모.
- Priority: 최대 6개. 카테고리·중요도·메모. 단순한 덤이 아니라 중요한 후보입니다.
- Transport: 최대 4명 출발역·동네, 합류 선호, 이동 우선순위. 실명/집 주소 대신 A·B와 역 이름을 권장합니다.
- Guardrail: 도보 부담, 최대 웨이팅, 귀가 시각, 혼잡, 1인 예산, 날씨 민감도. 미정은 정상 값입니다.
- 여백: 현장에서 정할 것을 남기는 보충 메모. 앱은 촘촘한 시간표를 만들지 않습니다.

조회 ID와 수정 키는 별개의 256비트 난수입니다. 수정 키의 SHA-256 해시만 DB에 저장합니다.
생성 직후 표시되는 수정 키를 별도로 보관하세요. 세션은 메모리에 있으므로 재시작 후 키로 권한을 복구합니다.
조회 링크만 가진 사람은 수정할 수 없습니다. CSRF와 HTML 이스케이프를 유지합니다.
출발지·이동 메모·AI 조사 결과는 계획자에게만 보입니다. 공유 URL은 기존과 동일합니다.

## Gemini 실제 조사

계획 저장 후 `계획 조사하기` 버튼 하나를 사용합니다. 백그라운드 작업은 서버당 하나만 실행합니다.

1. 구조화 입력과 익명화된 참여자 출발역을 Gemini에 전달합니다. 수정 키·조회 ID·실명·기존 비공개 이동 메모는 보내지 않습니다.
2. `/v1beta/interactions`에 `model`, `input`, `tools: [{"type":"google_search"}]`를 지정합니다. `tool_choice`는 사용하지 않습니다. Anchor 중심 핵심 검색 질문은 최대 3개를 요청합니다(프롬프트 지침이며 API의 검색 횟수 상한은 아닙니다).
3. `google_search_call` / 오류 없는 `google_search_result` / `url_citation` / 양수 `usage.grounding_tool_count` 중 검색 실행 흔적을 확인합니다. 추가로 유효한 HTTPS 인용 URL과 UTF-8 바이트 범위로 연결된 근거 문장이 필수입니다. 근거 없거나 검색 오류이면 추출 호출 없이 실패 처리합니다.
4. 같은 Interactions API의 두 번째 호출에는 도구 없이 `response_format`의 JSON Schema를 적용합니다. 계획과 검색 응답·인용 근거만 구조화하며 새 URL은 모델이 만들지 못합니다. 서버는 근거 ID를 검증하고 검색 실행 메타데이터·사용량을 resultJson에 함께 저장합니다.
5. Anchor 확인 / 위험·충돌 / Backup / Priority / 미확인 / 여백 / 출처를 표시합니다. EXACT Anchor의 대체 제안은 저장 결과에서 제외합니다.

각 조사 결과는 계획 버전과 연결합니다. 저장으로 버전이 바뀌면 이전 결과는 **재조사 필요**로 표시됩니다.
출처 있음은 검색 근거가 있다는 뜻이며, 실제 영업 여부를 보장하지 않습니다. 정보 불일치와 추론은 별도 상태입니다.
검색 제안 HTML은 별도 sandbox iframe으로 격리하며 메인 화면에는 모델 HTML을 삽입하지 않습니다.
조사 실패/시간 초과/할당량 부족은 계획 저장·수정·공유에 영향을 주지 않습니다. 자동 유료 모델 전환이나 자동 재시도는 없습니다.

### 설정

| 환경변수 | 기본값 | 역할 |
|---|---|---|
| `GEMINI_API_KEY` | 없음 | 서버 전용 Gemini 프로젝트 키 |
| `GEMINI_MODEL` | 없음 | 프로젝트에서 실제 사용 가능한 Gemini 3.x 모델 ID |
| `RESEARCH_ENABLED` | `false` | 결제·할당량 확인 후 `true`로 활성화 |
| `RESEARCH_DAILY_LIMIT` | `3` | 서버의 하루 조사 요청 수, 서울 날짜 기준 |
| `PORT` | `8080` | HTTP 포트 |

2026-10-08 실제 모델 목록에서 `gemini-3.8-flash`를 확인했습니다. 모델 기본값은 하드코딩하지 않습니다.
결제 활성화 후 최소 Interactions 요청에서 실제 검색 단계·인용·검색 사용량 반환을 확인했습니다.
하루 요청 수 제한은 금액 상한이 아닙니다. 조사 한 번은 모델 호출 2회와 여러 검색을 사용할 수 있습니다.
Google AI Studio에서 사용량·요금·할당량을 확인하세요. API 키 발급만으로 Search grounding이 활성화되지는 않습니다.

```powershell
$env:GEMINI_MODEL = 'gemini-3.8-flash'  # 위 날짜에 해당 프로젝트에서 확인한 ID
$env:RESEARCH_ENABLED = 'true'         # 결제/한도 준비 후에만
.\scripts\run-local.ps1
```

키는 채팅·소스·명령행 인수·Git에 넣지 마세요. Spring은 `.env`를 자동으로 읽지 않습니다.
공식 문서: [Search grounding](https://ai.google.dev/gemini-api/docs/google-search), [요금](https://ai.google.dev/gemini-api/docs/pricing), [API 키](https://ai.google.dev/gemini-api/docs/api-key).

## 데이터 모델

- `Plan`: 기존 조회/수정 권한, 기본 정보, 구조화 제약, 버전, 기존 메모.
- `PlanDetails`: JPA Embeddable Anchor / Candidate / Participant / Constraints 및 enum.
- `plan_anchors`, `plan_candidates`, `plan_participants`: Plan 소유의 단순 collection 테이블. 별도 서비스나 Repository 없음.
- `ResearchRun`: plan ID, plan version, status, startedAt, researchedAt, model, resultJson, errorMessage.
- V1은 그대로 유지. V2는 기존 Anchor 이름을 옮기고 나머지 선호를 미정으로 유지. V3는 조사 테이블 추가.

기존 자유 메모는 삭제/추론하지 않고 기존 메모 영역에 보존합니다. 새 계획은 구조화 항목으로 작성합니다.
운영 DB 변경 시 기존 migration을 수정하지 말고 새 버전을 추가합니다. `ddl-auto=validate`를 유지하세요.

## Render / PostgreSQL

GitHub 저장소: https://github.com/ludwig-han/yeobaek . Render 공개 주소: https://yeobaek.onrender.com/ .
재구성 시 Render Blueprint에서 `render.yaml`을 연결합니다. 실제 배포 브랜치와 환경변수는 Render 설정에서 확인합니다.

- Docker 빌드 단계에서 Java 17로 `mvn -B verify` 실행, 실행 JAR은 `target/app.jar`.
- `SPRING_PROFILES_ACTIVE=prod` 필수.
- `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` 필수. `DB_PORT` 기본 5432. Blueprint에서 DB로부터 주입.
- `GEMINI_API_KEY`, `GEMINI_MODEL`은 Render 환경변수에 직접 설정. 키를 YAML에 적지 않습니다.
- 애플리케이션의 `RESEARCH_ENABLED` 기본값은 false지만 현재 Blueprint는 true로 설정돼 있습니다. Gemini 키·모델·결제·할당량을 준비해야 조사가 동작합니다.
- 프로덕션은 Secure 세션 쿠키를 사용하므로 HTTPS로 접속합니다.
- `/healthz`는 HTTP 생존 확인이며 DB나 Gemini 상태를 검사하지 않습니다.
- Render 무료 플랜 제공/DB 보존 기간은 배포 시 계정 화면에서 확인하세요. 임의로 유료 전환하지 않습니다.

## 검증

```sh
mvn verify
```

PlanFlowTest: 생성·저장·공유·수정 키·권한·CSRF·충돌·입력 검증·XSS·404·구조화 저장·출발지 비공개.
MigrationTest: V1 데이터와 메모를 보존하며 업그레이드.
GeminiResearchClientTest: 로컬 HTTP 계약 테스트로 실제 요청 형식, 검색 없는 응답 거부, 잘못된 출처 거부, 할당량 실패, EXACT 대체 금지.
ResearchFlowTest: 조사 권한·저장·stale·개인 결과 비공개·실패 후 기존 기능 유지.
자동 테스트는 외부 API를 호출하지 않습니다. 테스트 대역은 테스트 코드에만 있습니다.

## 배포 후 인수 테스트

- 수원 예시 저장 → 수정 키 별도 보관 → 시크릿 창에서 공유 링크 확인.
- 출발역과 조사 결과가 공유 화면에 보이지 않는지 확인.
- 실제 조사를 실행해 출처·시각·검색어·미확인 표시를 확인. 실패는 성공으로 표시하면 안 됩니다.
- Anchor/웨이팅 수정 후 재조사 필요 표시 확인.
- 서버 재시작 후 계획과 결과 유지, 수정 키로 편집 복구 확인.
- 실제 PostgreSQL과 Gemini Search grounding 성공은 별도 검증 필요.

## 내 계획 (같은 브라우저)

홈의 ‘내 계획’에 직접 생성하거나 올바른 수정 키로 연 계획이 남습니다. 지난 계획도 유지되며, 계획자 화면을 다시 열면 제목·날짜·지역이 갱신됩니다. 공유 화면을 보는 것만으로는 목록에 추가되지 않습니다.

계획 본문은 서버 DB에, 목록과 수정 키는 해당 사이트의 브라우저 localStorage에 저장합니다. 목록에서 열 때 CSRF가 적용된 POST로 서버가 키를 확인하고 새 세션에 수정 권한을 복구합니다. 키는 URL에 넣지 않습니다. 서버는 키의 해시만 DB에 저장합니다.

브라우저 데이터 삭제·시크릿 모드 종료·기기 변경 시 목록이 이어지지 않습니다. 계획 주소와 수정 키를 따로 보관하세요. 기존 계획은 주소의 ‘수정 키로 열기’에서 복구하면 목록에 추가됩니다. 공용 브라우저에서는 보관된 키로 다른 사용자가 수정할 수 있습니다. 저장소가 차단되거나 가득 차면 안내를 표시하며 서버의 계획 저장은 유지됩니다. 도메인이 달라지면 별도 목록입니다.

2026-10-08 내 계획 검증: `mvn -B verify` 25개 테스트 통과. 로컬 파일 H2와 실제 브라우저에서 지역 없는 계획 생성, 목록 표시, 서버 재시작 후 목록에서 수정 권한 복구, 제목 변경 후 목록 갱신을 확인했습니다. 이 변경은 아직 Render에 배포하지 않았습니다.
