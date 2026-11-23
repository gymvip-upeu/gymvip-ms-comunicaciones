package com.gymvip.comms.api;

import com.gymvip.comms.infrastructure.MetaWhatsAppSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.Map;

@RestController
public class WhatsAppChannelStatusController {
    private final boolean enabled;
    private final String mode;
    private final String provider;
    private final MetaWhatsAppSender meta;

    public WhatsAppChannelStatusController(@Value("${whatsapp.enabled:true}") boolean enabled,
            @Value("${comms.mode:simulation}") String mode,
            @Value("${whatsapp.provider:meta}") String provider, MetaWhatsAppSender meta) {
        this.enabled=enabled; this.mode=mode; this.provider=provider; this.meta=meta;
    }

    @GetMapping("/api/v1/channels/status")
    public Map<String,Object> status() {
        boolean configured=meta.isConfigured();
        boolean simulation="simulation".equalsIgnoreCase(mode) || "simulation".equalsIgnoreCase(provider);
        boolean real="production".equalsIgnoreCase(mode) && "meta".equalsIgnoreCase(provider) && configured;
        String status=!enabled ? "DISABLED" : simulation ? "SIMULATION" : real ? "CONFIGURED_REAL" : "UNCONFIGURED";
        return Map.of("service","gymvip-comunicaciones","timestamp",Instant.now().toString(),
            "channels",Map.of("whatsapp",Map.of("channel","WHATSAPP","enabled",enabled,
                "status",status,"provider","meta".equalsIgnoreCase(provider) ? "Meta Cloud API" : "Simulación",
                "configurationComplete",configured,"connectionVerified",false,
                "effectiveMode",!enabled ? "DISABLED" : real ? "REAL" : simulation ? "SIMULATION" : "BLOCKED")));
    }
}
