# 003. [chore] PostgreSQL에서 스키마 업그레이드 자동 검증 추가

번호: 로컬 #003
상태: Open

유형: chore / test

## Background

prod 설정은 PostgreSQL을 사용하지만 현재 자동 테스트와 CI의 DB 검증은 H2 기반이다.

## Current behavior

기준 커밋: `db82c3d`. 기존 데이터가 있는 스키마의 업그레이드는 H2에서 검증한다. PostgreSQL을 실행하는 CI 작업은 없다.

## Acceptance Criteria

- [ ] 격리된 PostgreSQL에서 빈 DB에 전체 migration 적용을 검증한다.
- [ ] 이전 스키마의 계획·메모·시간 조건을 최신 스키마로 올려 보존 여부를 검증한다.
- [ ] 최신 스키마에서 애플리케이션의 스키마 검증이 통과한다.
- [ ] 개발자와 CI가 같은 검증을 실행할 수 있고 실행 조건이 간단히 기록된다.
- [ ] 운영 DB 접근이나 실제 Gemini 호출을 요구하지 않는다.

기존 migration 변경과 운영 배포는 이 작업의 범위 밖이다.