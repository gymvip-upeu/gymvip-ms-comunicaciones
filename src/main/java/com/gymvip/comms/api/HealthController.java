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
    private final boolean waEnabled;
    private final String waProvider;
    private final String emailMode;
    private final String emailProvider;
    private final MetaWhatsAppSender metaSender;
    private final ResendEmailSender resendSender;

    @org.springframework.beans.factory.annotation.Autowired
    public HealthController(
            @Value("${comms.mode:simulation}") String mode,
            @Value("${whatsapp.enabled:true}") boolean waEnabled,
            @Value("${whatsapp.provider:meta}") String waProvider,
            @Value("${email.mode:${comms.mode:simulation}}") String emailMode,
            @Value("${email.provider:simulation}") String emailProvider,
            MetaWhatsAppSender metaSender,
            ResendEmailSender resendSender
    ) {
        this.mode = mode;
        this.waEnabled = waEnabled;
        this.waProvider = waProvider;
        this.emailMode = emailMode;
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
                "emailMode", emailMode,
                "whatsappEnabled", waEnabled,
                "whatsappProvider", waProvider,
                "emailProvider", emailProvider,
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/api/v1/channels/status")
    public ResponseEntity<Map<String, Object>> channelsStatus() {
        boolean waSim = "simulation".equalsIgnoreCase(mode) || "simulation".equalsIgnoreCase(waProvider);
        boolean waConfigured = metaSender.isConfigured();
        boolean waReal = "production".equalsIgnoreCase(mode) && "meta".equalsIgnoreCase(waProvider) && waConfigured;
        String waStatus = !waEnabled ? "DISABLED" : waSim ? "SIMULATION" : waReal ? "CONFIGURED_REAL" : "UNCONFIGURED";

        boolean emailSim = "simulation".equalsIgnoreCase(emailMode) || "simulation".equalsIgnoreCase(emailProvider);
        boolean emailConfigured = resendSender.isConfigured();
        boolean emailReal = "production".equalsIgnoreCase(emailMode) && "resend".equalsIgnoreCase(emailProvider) && emailConfigured;
        String emailStatus = emailSim ? "SIMULATION" : emailReal ? "CONFIGURED_REAL" : "UNCONFIGURED";

        return ResponseEntity.ok(Map.of(
                "service", "gymvip-comunicaciones",
                "timestamp", Instant.now().toString(),
                "channels", Map.of(
                        "whatsapp", Map.of(
                                "channel", "WHATSAPP",
                                "enabled", waEnabled,
                                "status", waStatus,
                                "provider", "meta".equalsIgnoreCase(waProvider) ? "Meta Cloud API" : "Simulación",
                                "configurationComplete", waConfigured,
                                "connectionVerified", false,
                                "effectiveMode", !waEnabled ? "DISABLED" : waReal ? "REAL" : waSim ? "SIMULATION" : "BLOCKED"
                        ),
                        "email", Map.of(
                                "channel", "EMAIL",
                                "enabled", true,
                                "status", emailStatus,
                                "provider", "resend".equalsIgnoreCase(emailProvider) ? "Resend API" : "Simulación",
                                "configurationComplete", emailConfigured,
                                "effectiveMode", emailReal ? "REAL" : emailSim ? "SIMULATION" : "BLOCKED"
                        ),
                        "push", Map.of(
                                "channel", "PUSH",
                                "status", "MANAGED_BY_CORE",
                                "provider", "FCM",
                                "detail", "Notificaciones push despachadas por outbox de gymvip-backend hacia Android"
                        )
                )
        ));
    }
}
