package com.gymvip.comms.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
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

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "gymvip-comunicaciones",
                "status", "UP",
                "mode", mode,
                "whatsappProvider", provider,
                "timestamp", Instant.now().toString()
        ));
    }
}
