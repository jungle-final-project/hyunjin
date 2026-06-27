# 기획안 반영 점검

이 문서는 현재 프로토타입이 `PLAN (3).md`를 5인 팀 작업 시작점으로 적절히 반영했는지 확인합니다. 완성 기능 수준을 채점하지 않습니다.

| 최종 기획안 영역 | 프로토타입 반영 내용 | 상태 |
| --- | --- | --- |
| 도메인 모듈 | `user`, `build`, `part`, `agent`, `rag`, `log`, `ticket`, `price`, `admin` 백엔드 패키지가 존재함 | 반영 |
| 모듈 소유권 | 역할별 작업 공간 문서가 각 담당자의 폴더, API, 첫 PR 목표를 연결함 | 반영 |
| Seed 소유권 | 시드/모의 데이터가 하나의 공유 파일이 아니라 도메인별 시드 클래스로 분리됨 | 반영 |
| 프론트엔드 모듈 소유권 | 큰 quote/admin/common UI 파일이 담당자 기준 `pages`, `components`, `mocks`, `*Api.ts` 파일로 분리됨 | 반영 |
| 14개 이상 사용자/관리자 화면 | 17개 라우트 기본 검증 테스트가 소비자 및 관리자 구현 시작 화면을 검증함 | 반영 |
| AI/Agent 9단계 흐름 | 에이전트 상태 타임라인, RAG 검색, 도구 호출, 대체 응답, 관리자 근거 화면이 시작점으로 존재함 | 반영 |
| Tool Calling 정책 | 도구 결과 schema가 `PASS/WARN/FAIL`, confidence, warnings, evidence를 사용함 | 반영 |
| 부품/가격 범위 | 부품, 도구 검사, 목표가 알림, 가격 스냅샷 수집 작업, 관리자 가격 작업 임시 화면이 존재함 | 반영 |
| PC Agent/AS 범위 | Python CLI 골격, 로그 업로드 API, AS 티켓 API, 관리자 AS 상세, 동의, 30분 범위, 30일 보관 필드가 존재함 | 반영 |
| Infra 범위 | Docker Compose가 web, api, PostgreSQL pgvector, Redis, RabbitMQ, Mailpit을 포함하고 k6 기본 검증 스크립트가 존재함 | 반영 |
| PR 안전장치 | GitHub Actions가 web build, 라우트 기본 검증 테스트, API bootJar, Docker Compose config를 실행함 | 반영 |
| MVP 제외 항목 | README와 역할 규칙이 결제, 배송, 자체 원격제어, 정확한 FPS 보장, 최저가 보장을 범위 밖으로 유지함 | 반영 |

## 남은 비차단 항목

아래 항목은 5인 구현 착수의 차단 요소가 아닙니다.

- 각 담당자가 요청/응답 DTO를 확정하면서 OpenAPI schema를 더 엄격하게 만들 수 있습니다.
- 각 담당자가 service/repository 로직을 도입하기 전까지 controller는 시드 기반 응답을 반환합니다.
- 4번 담당자가 AS 흐름을 현재 두 화면보다 확장하면 지원 화면을 더 세분화할 수 있습니다.
