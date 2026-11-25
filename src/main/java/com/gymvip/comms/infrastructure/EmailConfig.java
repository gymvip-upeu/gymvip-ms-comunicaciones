package com.gymvip.comms.infrastructure;

import com.gymvip.comms.domain.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Selector automático del adaptador de Email según configuración y entorno.
 * HU-40.
 */
@Configuration
public class EmailConfig {

    private static final Logger log = LoggerFactory.getLogger(EmailConfig.class);

    @Bean
    @Primary
    public EmailSender primaryEmailSender(
            @Value("${email.mode:${comms.mode:simulation}}") String emailMode,
            @Value("${email.provider:simulation}") String provider,
            ResendEmailSender resendSender,
            SimulationEmailSender simulationSender
    ) {
        if ("production".equalsIgnoreCase(emailMode)) {
            if ("resend".equalsIgnoreCase(provider) && resendSender.isConfigured()) {
                log.info("[EMAIL CONFIG] Proveedor activo: RESEND API (Modo Producción)");
                return resendSender;
            }
            return (to, subject, template, variables) -> {
                throw new IllegalStateException("Correo real sin configurar. Revisa EMAIL_PROVIDER y RESEND_API_KEY.");
            };
        }

        log.info("[EMAIL CONFIG] Proveedor activo: SIMULATION EMAIL ADAPTER (Modo Seguro Sin Costos)");
        return simulationSender;
    }
}
