package com.gymvip.comms.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping
public class HealthController {

    private final String mode;
    private final String provider;

    public HealthController(
            @Value("${comms.mode:simulation}") String mode,
            @Value("${whatsapp.provider:meta}") String provider
    ) {
        this.mode = mode;
        this.provider = provider;
    }

    @GetMapping({"/api/v1/health", "/health"})
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "gymvip-comunicaciones",
                "status", "UP",
                "mode", mode,
                "whatsappProvider", provider,
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/api/v1/channels/status")
    public ResponseEntity<Map<String, Object>> channelsStatus() {
        boolean isSim = "simulation".equalsIgnoreCase(mode);
        String waStatus = isSim ? "SIMULATION" : "CONFIGURED_REAL";
        String emailStatus = isSim ? "SIMULATION" : "CONFIGURED_REAL";
        return ResponseEntity.ok(Map.of(
                "service", "gymvip-comunicaciones",
                "timestamp", Instant.now().toString(),
                "channels", Map.of(
                        "whatsapp", Map.of("status", waStatus, "provider", provider),
                        "email", Map.of("status", emailStatus, "provider", "Resend"),
                        "push", Map.of("status", "CONFIGURED_REAL", "provider", "FCM")
                )
        ));
    }
}
