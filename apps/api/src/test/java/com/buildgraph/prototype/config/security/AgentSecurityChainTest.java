package com.buildgraph.prototype.config.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.buildgraph.prototype.user.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(AgentEndpointProbeController.class)
@Import({AgentSecurityConfig.class, SecurityErrorResponseWriter.class})
class AgentSecurityChainTest {
    private static final String VALID_AGENT_TOKEN = "agent-valid-token";
    private static final String BAD_AGENT_TOKEN = "agent-bad-token";
    private static final String BLOCKED_AGENT_TOKEN = "agent-blocked-token";
    private static final String WEB_JWT_TOKEN = "jwt-user-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentTokenAuthenticationService agentTokenAuthenticationService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @Test
    void agentEndpointRejectsMissingBearerTokenWithUnauthorized() throws Exception {
        mockMvc.perform(post("/api/agent/devices/register"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void agentEndpointRejectsMissingBearerTokenOnAnyAgentPathWithUnauthorized() throws Exception {
        mockMvc.perform(post("/api/agent/heartbeat"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void agentEndpointRejectsBadAgentTokenWithUnauthorized() throws Exception {
        when(agentTokenAuthenticationService.authenticate(BAD_AGENT_TOKEN))
                .thenReturn(AgentTokenAuthenticationResult.invalid());

        mockMvc.perform(post("/api/agent/heartbeat")
                        .header("Authorization", "Bearer " + BAD_AGENT_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void agentEndpointRejectsWebJwtTokenAsInvalidAgentToken() throws Exception {
        when(agentTokenAuthenticationService.authenticate(WEB_JWT_TOKEN))
                .thenReturn(AgentTokenAuthenticationResult.invalid());

        mockMvc.perform(post("/api/agent/heartbeat")
                        .header("Authorization", "Bearer " + WEB_JWT_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void agentEndpointAcceptsValidAgentBearerToken() throws Exception {
        AgentPrincipal principal = new AgentPrincipal(10L, "device-public-id", 20L, "ACTIVE");
        when(agentTokenAuthenticationService.authenticate(VALID_AGENT_TOKEN))
                .thenReturn(AgentTokenAuthenticationResult.authenticated(principal));

        mockMvc.perform(post("/api/agent/heartbeat")
                        .header("Authorization", "Bearer " + VALID_AGENT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("device-public-id"))
                .andExpect(jsonPath("$.authenticationType").value("AgentAuthenticationToken"));
    }

    @Test
    void agentEndpointRejectsBlockedAgentTokenWithForbidden() throws Exception {
        when(agentTokenAuthenticationService.authenticate(BLOCKED_AGENT_TOKEN))
                .thenReturn(AgentTokenAuthenticationResult.forbidden("Agent device is not active."));

        mockMvc.perform(post("/api/agent/heartbeat")
                        .header("Authorization", "Bearer " + BLOCKED_AGENT_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void agentTokenDoesNotAuthenticateWebJwtProtectedEndpoint() throws Exception {
        when(currentUserService.requireUser("Bearer " + VALID_AGENT_TOKEN))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."));

        mockMvc.perform(get("/api/web-jwt-protected")
                        .header("Authorization", "Bearer " + VALID_AGENT_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
