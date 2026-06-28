package com.buildgraph.prototype.agent;

import com.buildgraph.prototype.common.DbValueMapper;
import com.buildgraph.prototype.common.MockData;
import com.buildgraph.prototype.rag.RagQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AgentQueryService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final JdbcTemplate jdbcTemplate;
    private final RagQueryService ragQueryService;

    public AgentQueryService(JdbcTemplate jdbcTemplate, RagQueryService ragQueryService) {
        this.jdbcTemplate = jdbcTemplate;
        this.ragQueryService = ragQueryService;
    }

    public Map<String, Object> createSession(Map<String, Object> request) {
        String requirementId = stringOrNull(request == null ? null : request.get("requirementId"));
        String buildId = stringOrNull(request == null ? null : request.get("buildId"));
        String asTicketId = stringOrNull(request == null ? null : request.get("asTicketId"));
        int rootCount = (requirementId == null ? 0 : 1) + (buildId == null ? 0 : 1) + (asTicketId == null ? 0 : 1);
        if (rootCount != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requirementId, buildId, asTicketId 중 정확히 하나만 보내야 합니다.");
        }
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                INSERT INTO agent_sessions (
                  user_id,
                  requirement_id,
                  build_id,
                  as_ticket_id,
                  status,
                  state_timeline
                )
                VALUES (
                  (SELECT id FROM users WHERE email = 'user@example.com'),
                  (SELECT id FROM requirements WHERE public_id = ?::uuid),
                  (SELECT id FROM builds WHERE public_id = ?::uuid),
                  (SELECT id FROM as_tickets WHERE public_id = ?::uuid),
                  'QUEUED',
                  ?::jsonb
                )
                RETURNING public_id::text AS id, status, created_at
                """, requirementId, buildId, asTicketId, json(List.of(timelineItem(null, "QUEUED", "USER", "session created"))));
        String id = DbValueMapper.string(row, "id");
        return session(id);
    }

    public Map<String, Object> runSession(String id) {
        Map<String, Object> row = agentSessionRow(id);
        String currentStatus = DbValueMapper.string(row, "status");
        if (!"QUEUED".equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "QUEUED 상태의 Agent session만 실행할 수 있습니다.");
        }
        List<Object> timeline = appendTimeline(row, currentStatus, "RUNNING", "SYSTEM", "agent run requested");
        jdbcTemplate.update("""
                UPDATE agent_sessions
                SET status = 'RUNNING',
                    state_timeline = ?::jsonb,
                    updated_at = now()
                WHERE public_id = ?::uuid
                """, json(timeline), id);
        return session(id);
    }

    public Map<String, Object> session(String id) {
        Map<String, Object> row = agentSessionRow(id);
        return MockData.map(
                "id", DbValueMapper.string(row, "id"),
                "status", DbValueMapper.string(row, "status"),
                "stateTimeline", DbValueMapper.json(row, "state_timeline", List.of()),
                "summary", DbValueMapper.string(row, "summary"),
                "toolInvocationIds", toolInvocationIdsBySession(id),
                "evidenceIds", evidenceIdsBySession(id)
        );
    }

    public Map<String, Object> adminSession(String id) {
        Map<String, Object> row = agentSessionRow(id);
        return MockData.map(
                "id", DbValueMapper.string(row, "id"),
                "status", DbValueMapper.string(row, "status"),
                "summary", DbValueMapper.string(row, "summary"),
                "stateTimeline", DbValueMapper.json(row, "state_timeline", List.of()),
                "toolInvocations", toolInvocationsBySession(id),
                "evidenceIds", evidenceIdsBySession(id)
        );
    }

    public Map<String, Object> agentSessions() {
        List<Map<String, Object>> items = jdbcTemplate.queryForList("""
                        SELECT s.public_id::text AS id,
                               s.status,
                               u.public_id::text AS user_id,
                               s.created_at
                        FROM agent_sessions s
                        JOIN users u ON u.id = s.user_id
                        ORDER BY s.created_at DESC, s.id DESC
                        """)
                .stream()
                .map(row -> MockData.map(
                        "id", DbValueMapper.string(row, "id"),
                        "status", DbValueMapper.string(row, "status"),
                        "userId", DbValueMapper.string(row, "user_id"),
                        "createdAt", DbValueMapper.timestamp(row, "created_at")
                ))
                .toList();
        return MockData.map("items", items, "page", 0, "size", 20, "total", items.size());
    }

    public Map<String, Object> toolInvocations() {
        List<Map<String, Object>> items = jdbcTemplate.queryForList(toolInvocationSql() + " ORDER BY ti.created_at DESC, ti.id DESC")
                .stream()
                .map(this::toolInvocationMap)
                .toList();
        return MockData.map("items", items, "page", 0, "size", 20, "total", items.size());
    }

    public Map<String, Object> toolInvocation(String id) {
        return jdbcTemplate.queryForList(toolInvocationSql() + " WHERE ti.public_id = ?::uuid", id)
                .stream()
                .findFirst()
                .map(this::toolInvocationMap)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tool invocation을 찾을 수 없습니다."));
    }

    private Map<String, Object> agentSessionRow(String id) {
        return jdbcTemplate.queryForList("""
                        SELECT public_id::text AS id, status, summary, state_timeline, created_at, updated_at
                        FROM agent_sessions
                        WHERE public_id = ?::uuid
                        """, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent session을 찾을 수 없습니다."));
    }

    private List<Map<String, Object>> toolInvocationsBySession(String sessionId) {
        return jdbcTemplate.queryForList(toolInvocationSql() + " WHERE s.public_id = ?::uuid ORDER BY ti.id", sessionId)
                .stream()
                .map(this::toolInvocationMap)
                .toList();
    }

    private List<Object> toolInvocationIdsBySession(String sessionId) {
        return toolInvocationsBySession(sessionId).stream().map(invocation -> invocation.get("id")).toList();
    }

    private List<Object> evidenceIdsBySession(String sessionId) {
        return ragQueryService.evidenceBySession(sessionId).stream().map(evidence -> evidence.get("id")).toList();
    }

    private String toolInvocationSql() {
        return """
                SELECT ti.public_id::text AS id,
                       s.public_id::text AS agent_session_id,
                       ti.tool_name,
                       ti.status,
                       ti.confidence,
                       ti.summary,
                       ti.request_payload,
                       ti.result_payload,
                       ti.latency_ms,
                       ti.created_at
                FROM tool_invocations ti
                JOIN agent_sessions s ON s.id = ti.agent_session_id
                """;
    }

    private Map<String, Object> toolInvocationMap(Map<String, Object> row) {
        return MockData.map(
                "id", DbValueMapper.string(row, "id"),
                "agentSessionId", DbValueMapper.string(row, "agent_session_id"),
                "toolName", DbValueMapper.string(row, "tool_name"),
                "status", DbValueMapper.string(row, "status"),
                "confidence", DbValueMapper.string(row, "confidence"),
                "summary", DbValueMapper.string(row, "summary"),
                "latencyMs", DbValueMapper.integer(row, "latency_ms"),
                "requestPayload", DbValueMapper.json(row, "request_payload", Map.of()),
                "resultPayload", DbValueMapper.json(row, "result_payload", Map.of()),
                "createdAt", DbValueMapper.timestamp(row, "created_at")
        );
    }

    private static Map<String, Object> timelineItem(String from, String to, String actor, String reason) {
        return MockData.map("from", from, "to", to, "at", MockData.now(), "actor", actor, "reason", reason);
    }

    private static List<Object> appendTimeline(Map<String, Object> row, String from, String to, String actor, String reason) {
        List<Object> timeline = new ArrayList<>();
        Object currentTimeline = DbValueMapper.json(row, "state_timeline", List.of());
        if (currentTimeline instanceof List<?> currentItems) {
            timeline.addAll(currentItems);
        }
        timeline.add(timelineItem(from, to, actor, reason));
        return timeline;
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : value.toString();
    }

    private static String json(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 변환에 실패했습니다.", e);
        }
    }
}
