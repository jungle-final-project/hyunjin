# Sprint 1 시작 체크리스트

이 체크리스트는 최종 기획안을 완성 기능 구현이 아니라 실제 작업 착수 단위로 바꾸기 위한 문서입니다.

## 공통

- 각 담당자가 `docker compose up --build`를 실행할 수 있는지 확인합니다.
- 우선 자신의 담당 패키지 또는 기능 폴더 안에서 작업합니다.
- 요청 또는 응답 필드가 바뀌면 `docs/openapi.yaml`을 함께 수정합니다.
- 화면을 추가할 때 라우트 기본 검증 테스트가 계속 통과하도록 유지합니다.

## 1번: 견적/인증

- 로그인/회원가입 폼 필드를 실제 상태에 연결합니다.
- 정적 견적 동작을 `/api/requirements/parse`와 `/api/builds/recommend` 호출로 교체합니다.
- 기존 화면에 로딩, 오류, 성공 UI 상태를 추가합니다.

## 2번: 부품/가격

- 부품 목록과 도구 검사를 위한 DTO/service 경계를 추가합니다.
- 호환성, 전력, 크기, 성능, 가격 도구의 규칙 입력 구조를 정의합니다.
- `/api/price-snapshots/collect`를 가격 수집 작업 연결의 시작점으로 유지합니다.

## 3번: Agent/RAG

- 세션 상태 전이를 구현합니다. `QUEUED -> RUNNING -> RAG_SEARCHED -> TOOLS_CALLED -> SUMMARY_READY`
- 대체 응답 경로를 추가합니다. `FAILED -> FALLBACK_READY`
- 이후 RAG 시드 데이터를 pgvector 기반 근거 검색으로 교체합니다.
- `/admin/agent-sessions/:id`, `/admin/tool-invocations/:id`, `/admin/rag-evidence/:id`를 검토 화면으로 사용합니다.

## 4번: PC Agent/AS

- 더 많은 지표를 추가하기 전에 JSONL 형식을 안정적으로 유지합니다.
- 명시적 업로드 동의와 최근 30분 내보내기 범위를 강제합니다.
- 원인 후보는 관리자에게만 보이도록 합니다.

## 5번: Infra/Admin/Auth

- 사용자/관리자 라우트 보호 로직을 추가합니다.
- `.github/workflows/ci.yml`에서 PR 빌드, 라우트 기본 검증, API bootJar, compose config 검사를 유지합니다.
- k6 스크립트 골격을 기획된 300/1000 동시 사용자 검사 방향으로 확장합니다.
- Redis, RabbitMQ, Mailpit, PostgreSQL이 Docker Compose로 재현 가능하게 시작되도록 유지합니다.
