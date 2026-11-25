package com.gymvip.comms.api;

import com.gymvip.comms.infrastructure.MetaWhatsAppSender;
import com.gymvip.comms.infrastructure.ResendEmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping
public class HealthController {

    private final String mode;
    private final String waProvider;
    private final String emailProvider;
    private final MetaWhatsAppSender metaSender;
    private final ResendEmailSender resendSender;

    public HealthController(
            @Value("${comms.mode:simulation}") String mode,
            @Value("${whatsapp.provider:meta}") String waProvider,
            @Value("${email.provider:simulation}") String emailProvider,
            MetaWhatsAppSender metaSender,
            ResendEmailSender resendSender
    ) {
        this.mode = mode;
        this.waProvider = waProvider;
        this.emailProvider = emailProvider;
        this.metaSender = metaSender;
        this.resendSender = resendSender;
    }

    @RequestMapping(value = "/", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "service", "gymvip-comunicaciones",
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping({"/api/v1/health", "/health"})
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "gymvip-comunicaciones",
                "status", "UP",
                "mode", mode,
                "whatsappProvider", waProvider,
                "emailProvider", emailProvider,
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/api/v1/channels/status")
    public ResponseEntity<Map<String, Object>> channelsStatus() {
        boolean isGlobalSim = "simulation".equalsIgnoreCase(mode);

        String waStatus;
        if (isGlobalSim || "simulation".equalsIgnoreCase(waProvider)) {
            waStatus = "SIMULATION";
        } else if (metaSender.isConfigured()) {
            waStatus = "CONFIGURED_REAL";
        } else {
            waStatus = "MISSING_CREDENTIALS";
        }

        String emailStatus;
        if (isGlobalSim || "simulation".equalsIgnoreCase(emailProvider)) {
            emailStatus = "SIMULATION";
        } else if (resendSender.isConfigured()) {
            emailStatus = "CONFIGURED_REAL";
        } else {
            emailStatus = "MISSING_CREDENTIALS";
        }

        return ResponseEntity.ok(Map.of(
                "service", "gymvip-comunicaciones",
                "timestamp", Instant.now().toString(),
                "channels", Map.of(
                        "whatsapp", Map.of("status", waStatus, "provider", waProvider),
                        "email", Map.of("status", emailStatus, "provider", emailProvider),
                        "push", Map.of(
                                "status", "UNAVAILABLE_ON_SERVER",
                                "provider", "FCM",
                                "detail", "Notificaciones push gestionadas en cliente Android"
                        )
                )
        ));
    }
}
