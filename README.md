# BuildGraph AI 프로토타입

정글 최종 프로젝트용 BuildGraph AI 프로토타입 모노레포입니다.

## 기술 스택

- 웹: React, TypeScript, Vite, Tailwind, React Router, TanStack Query
- API: Spring Boot, Gradle, Java 21
- 인프라: PostgreSQL + pgvector, Redis, RabbitMQ, Mailpit, Docker Compose
- PC 에이전트: Python 3.11 CLI 골격

## 빠른 실행

```powershell
docker compose up --build
```

- 웹: http://localhost:5173
- API: http://localhost:8080/api/health
- RabbitMQ: http://localhost:15672
- Mailpit: http://localhost:8025

## 검증

```powershell
cd apps/web
npm run build
npm run test

cd ../..
docker compose config
docker compose up --build
```

선택 사항으로 k6 시작 스크립트를 실행할 수 있습니다.

```powershell
k6 run infra/k6/smoke.js
```

GitHub Actions는 풀 리퀘스트와 `main` 브랜치 푸시에서 같은 핵심 검사를 실행합니다.

- `apps/web`: `npm ci`, `npm run build`, `npm run test`
- `apps/api`: Java 21 `./gradlew bootJar --no-daemon`
- 루트: `docker compose config`

## 프로토타입 범위

이 저장소는 데스크톱 전용 프로토타입 스캐폴드입니다. 14개 핵심 사용자/관리자 화면, 시드 기반 API 응답, DB 연결 런타임 구성, 5인 역할별 작업 공간을 연결합니다.

이번 스캐폴드 범위에서 제외하는 항목은 실제 결제/배송, 자체 원격제어, 정확한 FPS 보장, 최저가 보장, 운영용 AI/RAG 외부 연동입니다.
