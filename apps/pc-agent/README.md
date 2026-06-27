# BuildGraph PC 에이전트 골격

AS 업로드 테스트에 사용할 JSON Lines 형식의 하드웨어 로그를 생성하고 최근 구간만 내보내는 프로토타입 전용 Python CLI입니다.

## 명령어

```powershell
python buildgraph_agent.py sample --out ../../seed/sample-agent-log.jsonl
python buildgraph_agent.py export --source ../../seed/sample-agent-log.jsonl --out recent-30m.jsonl --minutes 30
```

이 MVP 골격은 백그라운드 서비스를 설치하지 않습니다. 프론트엔드/백엔드 AS 흐름 테스트를 위한 결정적 로컬 로그만 생성합니다.
