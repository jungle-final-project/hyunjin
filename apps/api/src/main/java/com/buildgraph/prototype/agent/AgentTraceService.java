package com.buildgraph.prototype.agent;

import com.buildgraph.prototype.common.DbValueMapper;
import com.buildgraph.prototype.common.MockData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AgentTraceService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final JdbcTemplate jdbcTemplate;

    public AgentTraceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String createQueuedSession(AgentSessionRoot root, String actor) {
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
                RETURNING public_id::text AS id
                """,
                root.requirementId(),
                root.buildId(),
                root.asTicketId(),
                json(List.of(timelineItem(null, "QUEUED", actor, "session created for " + root.purpose()))));
        return DbValueMapper.string(row, "id");
    }

    static Map<String, Object> timelineItem(String from, String to, String actor, String reason) {
        return MockData.map("from", from, "to", to, "at", MockData.now(), "actor", actor, "reason", reason);
    }

    static String json(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 변환에 실패했습니다.", e);
        }
    }
}
