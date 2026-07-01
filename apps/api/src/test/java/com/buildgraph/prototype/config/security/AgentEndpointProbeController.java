package com.buildgraph.prototype.config.security;

import com.buildgraph.prototype.user.CurrentUserService;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class AgentEndpointProbeController {
    private final CurrentUserService currentUserService;

    AgentEndpointProbeController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @PostMapping("/agent/heartbeat")
    Object heartbeat(@AuthenticationPrincipal AgentPrincipal principal, Authentication authentication) {
        return Map.of(
                "deviceId", principal.deviceId(),
                "authenticationType", authentication.getClass().getSimpleName()
        );
    }

    @GetMapping("/web-jwt-protected")
    Object webJwtProtected(@RequestHeader(value = "Authorization", required = false) String authorization) {
        CurrentUserService.CurrentUser user = currentUserService.requireUser(authorization);
        return Map.of("userId", user.id());
    }
}
