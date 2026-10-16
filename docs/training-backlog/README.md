# 작업 백로그

GitHub 연동의 Issue 생성 요청이 권한 오류(403, Resource not accessible by integration)로 거절되어 저장소에서 관리한다.
아래 번호는 GitHub Issue 번호가 아닌 로컬 백로그 번호다.

| 번호 | 작업 | 담당 |
|---|---|---|
| [#001](001-validation-errors.md) | 빈 선택값을 제출하면 계획 입력 오류 화면 대신 500이 반환됨 | ludwig-han · 첫 배정 |
| [#002](002-unreadable-research.md) | 저장된 조사 결과를 읽지 못한 경우 계획자에게 안내 | 미배정 |
| [#003](003-postgresql-migrations.md) | PostgreSQL에서 스키마 업그레이드 자동 검증 추가 | 미배정 |
| [#004](004-research-limits.md) | 조사 요청 한도와 중단 처리의 경계 조건 검증 | 미배정 |
| [#005](005-browser-library.md) | 내 계획 목록과 수정 권한 복구의 브라우저 회귀 검증 | 미배정 |

기준 커밋은 `db82c3d`. 2026-10-16 로컬 Java 17에서 `mvn -B verify`의 기존 테스트 38개가 통과했다.
작업별 완료 조건은 각 Issue 본문을 따른다.
