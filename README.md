# 여백 v0.1

Java 17 · Spring Boot 3.5.16 · Thymeleaf · JPA · Flyway.
핵심 목표 1~2개를 지키고, 나머지는 현장에서 정하는 하루 계획 도구입니다.

## 현재 검증 상태

구현과 배포 설정을 작성했습니다. 작성 환경에는 Maven/Docker가 없고 Maven Central 연결이 시간 초과되어 **Spring Boot 빌드·통합 테스트·실행·실제 배포는 아직 검증하지 못했습니다.**
`mvn verify` 또는 Docker 빌드 성공 후 아래 인수 테스트까지 완료해야 내일 사용 준비가 끝납니다. 실행 가능한 공개 URL은 아직 발급되지 않았습니다.

## 가장 빠른 로컬 실행

1. 이 폴더의 `pom.xml`을 IntelliJ에서 프로젝트로 엽니다.
2. 프로젝트 SDK를 Java 17 이상으로 지정하고 Maven 의존성을 불러옵니다. IntelliJ의 Bundled Maven을 사용할 수 있습니다.
3. Maven 패널 → Lifecycle → **verify**를 실행합니다. 테스트를 생략하지 마세요.
4. `YeobaekApplication.main()`을 실행합니다.
5. 브라우저에서 http://localhost:8080 을 엽니다.

Maven이 설치되어 있다면:

```sh
mvn verify
mvn spring-boot:run
```

또는 Docker가 있다면:

```sh
docker build -t yeobaek .
docker run --rm -p 8080:8080 -v yeobaek-data:/app/data yeobaek
```

H2 파일은 로컬 `data/`에 저장됩니다. 이 폴더는 Git에 올리지 마세요. Docker 실행은 위와 같이 볼륨을 붙여야 재시작 후 계획이 유지됩니다.

## 배포: GitHub → Render Blueprint

배포 계정은 작성 환경에 연결되어 있지 않습니다. 호택님 계정에서 다음 단계를 진행해야 합니다. API 키나 비밀번호를 대화에 붙여넣지 마세요.

1. GitHub에 빈 저장소를 생성합니다. README 자동 생성 옵션은 끕니다.
2. 압축을 푼 `yeobaek` 폴더에서 아래 명령을 실행합니다. `YOUR_REPOSITORY_URL`은 본인 저장소 주소로 바꿉니다.

```sh
git init
git add .
git commit -m "Build first usable day planner"
git branch -M main
git remote add origin YOUR_REPOSITORY_URL
git push -u origin main
```

3. Render에 로그인 → New → **Blueprint** → 해당 GitHub 저장소 연결 → `render.yaml` 확인.
4. 웹 서비스와 PostgreSQL이 모두 **Free**로 표시되는지 확인한 뒤 생성합니다. Free를 제공하지 않는 계정/상황이라면 유료로 변경하기 전에 선택을 멈추고 비용을 확인하세요.
5. Docker 빌드가 `mvn -B verify`를 실행합니다. 실패하면 Render 로그에서 첫 번째 `ERROR`부터 확인합니다. 테스트를 건너뛰어 배포하지 마세요.
6. 서비스가 Live가 되면 Render가 표시한 **실제 URL**을 엽니다. 임의로 도메인을 추측하지 마세요.
7. 아래 인수 테스트를 실행합니다. 실제 수원 계획을 저장하고 수정 키를 별도로 보관합니다.

배포 DB는 Render PostgreSQL이며 웹 서버의 임시 디스크와 분리되어 있습니다. 앱과 DB는 같은 Singapore 리전에 놓입니다. `prod` 프로파일은 필수 DB 환경변수가 없으면 시작하지 않도록 구성했습니다.

Render 무료 웹 서비스는 유휴 시 중단되어 첫 접속이 느릴 수 있습니다. 무료 PostgreSQL은 생성 후 **30일에 만료**됩니다. 내일 검증용 출발점이며 장기 운영 전 DB 백업·이전/플랜 결정을 해야 합니다.

공식 문서:
- https://render.com/docs/docker
- https://render.com/docs/blueprint-spec
- https://render.com/docs/free

## 내일 사용 전 인수 테스트 (약 5분)

- [ ] 첫 화면에서 `10월 8일 수원 계획으로 시작하기`로 실제 계획을 입력합니다.
- [ ] 합류 시간/장소와 필요한 운영 정보를 직접 확인해 보완하고 저장합니다.
- [ ] 수정 키를 비공개 메모에 보관하고 계획 페이지를 북마크합니다.
- [ ] 새로고침 후 계획이 남아 있는지 확인합니다.
- [ ] 공유 링크를 시크릿 창 또는 다른 휴대폰에서 열고, 이동 검토 메모가 보이지 않는지 확인합니다.
- [ ] 수정 키 없이 수정할 수 없는지 확인합니다.
- [ ] 원래 브라우저에서 수정 후 공유 화면을 새로고침해 반영을 확인합니다.
- [ ] 서버 재시작 후 공유 링크가 유지되는지 확인하고 수정 키로 수정 화면을 복구합니다.
- [ ] 실제 동행자에게 공유 링크만 보내고 함께 열어봅니다. 공유는 앱이 자동 전송하지 않습니다.

## 최소 모델과 데이터 흐름

`Plan` 한 테이블에 모든 내용을 저장합니다. 후보·주의·대안 등을 별도 엔티티로 나누지 않습니다.

| 필드 | 역할 |
|---|---|
| id | 256비트 무작위 조회 식별자 |
| editKeyHash | 별도 256비트 수정 키의 SHA-256 해시 |
| title, date, region | 기본 정보 |
| anchor1, anchor2 | 첫 목표 필수, 둘째 목표 선택 |
| meeting | 공유되는 만남·준비물 안내 |
| transport | 계획자만 보는 출발지·합류 검토 |
| priorities, guardrails, backup, flexible | 선택 입력하는 메모 |
| version | 오래된 화면의 덮어쓰기 방지 |
| createdAt, updatedAt | 생성·수정 시각 |

POST `/plans` → Controller의 DTO 검증 → Service에서 토큰 생성 → Repository 저장 → 상세 화면으로 redirect.

POST `/p/{id}` → 세션의 수정 권한 확인 → DTO 검증 → Service의 버전 확인 → 트랜잭션에서 변경 → 상세 화면으로 redirect.

핵심 설계 판단:
- 로그인 대신 **조회 링크와 수정 키를 분리**했습니다. 생성 직후 받은 키로 다른 브라우저에서도 수정 권한을 복구할 수 있습니다.
- 세션은 메모리에 있습니다. 서버가 재시작하면 수정 권한은 사라지지만 **계획은 DB에 남고, 수정 키로 권한을 복구**합니다.
- 키는 URL에 넣지 않고 POST로 제출합니다. DB에는 원문 대신 해시를 저장합니다. 사용자 암호가 아니라 충분히 긴 난수이므로 빠른 SHA-256을 사용했습니다.
- CSRF 보호를 유지하고 Thymeleaf의 `th:text`로 사용자 입력을 이스케이프합니다.
- 공유 화면과 일반 익명 상세 화면은 transport를 출력하지 않습니다. 나머지 내용은 조회 링크를 가진 누구나 볼 수 있습니다. 개인적인 주소는 transport에만 적고 공유 메모에 넣지 마세요.
- 조회 목록, 회원, 지도, 외부 API는 없습니다. 링크를 잃으면 검색·복구할 수 없습니다.
- 출처·확인 시각·대체 가능 여부·전환 조건은 지금은 메모에 적습니다.

## 코드 읽는 순서

1. `PlanForm` → 어떤 입력을 허용하는지
2. `PlanController` → 요청과 화면의 연결, 수정 권한 확인
3. `PlanService` → 저장·키 검증·충돌 방지
4. `Plan` / `PlanRepository` → DB 구조
5. `templates/plan.html` → 공유 화면에서 무엇을 숨기는지
6. `PlanFlowTest` → 사용자 흐름과 권한 경계

## 테스트 및 운영

`PlanFlowTest`는 생성·공유·수정·수정 키 복구·권한 거부·CSRF·잘못된 입력·동시 편집 충돌·XSS 이스케이프·404를 검증합니다. H2 기반 테스트이며 실제 PostgreSQL은 배포 후 인수 테스트로 추가 확인해야 합니다.

서버 로그에는 `plan_created`, `plan_updated` 이벤트만 추가했습니다. 개인정보와 조회/수정 키는 기록하지 않습니다. 프록시의 URL 로그에는 조회 식별자가 남을 수 있으므로 조회 링크를 완전한 비밀 보관 수단으로 취급하지 마세요.

DB 변경은 `V1__create_plans.sql`을 수정하지 않고 `V2__...sql`을 새로 작성합니다. 운영 DB가 만들어진 뒤 `ddl-auto=create`로 바꾸면 안 됩니다. Flyway로 적용하고 Hibernate가 모델을 검증합니다.

문제 발생 시: `/healthz` → Render 앱 로그 → DB 연결 상태 순서로 확인합니다. DB를 초기화하지 마세요. 잘못된 배포는 이전 커밋으로 되돌리되 이미 적용한 DB 마이그레이션과 호환되는지 먼저 확인합니다.

이번 v0.1은 소수에게 링크로 전달하는 검증용입니다. 대중 공개/홍보 전에 생성 요청 제한, 삭제·보관 정책, 키 교체/공유 해제는 실제 필요와 함께 검토해야 합니다.

## 10/8 사용 후: 관찰 3개만

1. 현장에서 이 화면을 언제 다시 열었나요? 안 열었다면 왜였나요?
2. 카톡 메모보다 도움이 된 점 / 더 번거로웠던 점은 무엇인가요?
3. 실제로 놓쳤거나 위험했던 정보 한 가지는 무엇인가요?

메신저로 피드백을 받고 발생 상황·불편·다음 수정 한 가지를 적습니다. 그중 하나를 고쳐 v0.2로 배포합니다. 입력칸을 더 늘리거나 추천 기능을 붙이는 것은 이 관찰 전에는 하지 않습니다.

## TODO — 피드백 전에는 구현하지 않기

- 체크 완료 표시가 정말 필요한지
- 출처/확인시각을 별도 필드로 분리할지
- 공유 화면에서 즉시 보여야 할 주의사항이 무엇인지
- 데이터 삭제·공유 해제·키 교체
- 다음 사용자 2명에게도 반복 사용 가능한지 (전체 목표: 본인 외 3명)
