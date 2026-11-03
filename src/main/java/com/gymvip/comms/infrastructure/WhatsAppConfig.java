package com.gymvip.comms.infrastructure;

import com.gymvip.comms.domain.WhatsAppSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Selector automático del adaptador de WhatsApp según configuración y entorno.
 * HU-17 T03.
 */
@Configuration
public class WhatsAppConfig {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppConfig.class);

    @Bean
    @Primary
    public WhatsAppSender primaryWhatsAppSender(
            @Value("${comms.mode:simulation}") String commsMode,
            @Value("${whatsapp.provider:meta}") String provider,
            MetaWhatsAppSender metaSender,
            SimulationWhatsAppSender simulationSender
    ) {
        if ("production".equalsIgnoreCase(commsMode) && "meta".equalsIgnoreCase(provider) && metaSender.isConfigured()) {
            log.info("[WHATSAPP CONFIG] Proveedor activo: META CLOUD API (Modo Producción)");
            return metaSender;
        }

        log.info("[WHATSAPP CONFIG] Proveedor activo: SIMULATION ADAPTER (Modo Seguro Sin Costos)");
        return simulationSender;
    }
}
