package com.gymvip.comms.infrastructure;

import com.gymvip.comms.domain.WhatsAppSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class WhatsAppConfig {
    @Bean
    @Primary
    public WhatsAppSender primaryWhatsAppSender(
            @Value("${comms.mode:simulation}") String commsMode,
            @Value("${whatsapp.provider:meta}") String provider,
            @Value("${whatsapp.enabled:true}") boolean enabled,
            MetaWhatsAppSender metaSender, SimulationWhatsAppSender simulationSender) {
        if (!enabled) return request -> WhatsAppSender.SendResult.failure("Canal WhatsApp desactivado", false);
        if ("simulation".equalsIgnoreCase(commsMode) || "simulation".equalsIgnoreCase(provider)) return simulationSender;
        // Production Meta must report errors explicitly, including missing credentials.
        if ("production".equalsIgnoreCase(commsMode) && "meta".equalsIgnoreCase(provider)) return metaSender;
        return request -> WhatsAppSender.SendResult.failure("Configuración de WhatsApp no válida", false);
    }
}
